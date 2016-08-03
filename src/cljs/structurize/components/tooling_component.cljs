(ns structurize.components.tooling-component
  (:require [structurize.components.component-utils :as u]
            [structurize.components.general :as g]
            [reagent.core :as r]
            [traversy.lens :as l]
            [taoensso.timbre :as log]))

(declare node-group)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; app-browser components


(defn node [{:keys [config-opts track-app track-tooling side-effect!] :as φ} path _]
  (let [log? (get-in config-opts [:tooling :log?])
        toggle-collapsed #(side-effect! [:tooling/toggle-node-collapsed {:path path}])
        toggle-focused #(side-effect! [:tooling/toggle-node-focused {:path path}])]

    (when log? (log/debug "mount node:" path))

    (fn [_ _ opts]
      (let [{:keys [tail-braces first? last?]} opts
            node (track-app l/view-single (l/in path))
            collapsed? (track-tooling l/view-single (l/in [:app-browser-props :collapsed])
                                      #(contains? % path))
            written? (track-tooling l/view-single (l/in [:app-browser-props :written :paths])
                                    #(contains? % path))
            upstream-written? (track-tooling l/view-single (l/in [:app-browser-props :written :upstream-paths])
                                             #(contains? % path))
            focused? (track-tooling l/view-single (l/in [:app-browser-props :focused :paths])
                                    #(contains? % path))
            upstream-focused? (track-tooling l/view-single (l/in [:app-browser-props :focused :upstream-paths])
                                             #(contains? % path))
            k (last path)
            v node
            collapsed-group-node? (and collapsed?
                                       (map? v)
                                       (not (empty? node)))
            empty-group-node? (and (map? v) (empty? node))
            node-value? (not (map? v))
            show-tail-braces? (and last? (or collapsed? empty-group-node? node-value?))
            node-key-class (u/->class
                            (cond-> #{:clickable}
                              focused? (conj :focused)
                              upstream-focused? (conj :upstream-focused)
                              written? (conj :written)
                              upstream-written? (conj :upstream-written)
                              first? (conj :first)))
            node-value-class (u/->class (cond-> #{}
                                          focused? (conj :focused)
                                          written? (conj :written)
                                          upstream-written? (conj :upstream-written)))
            node-group-class (if focused? :focused)]

        (when log? (log/debug "render node:" path))

        [:div.node
         [:div.node-brace {:class (when-not first? :hidden)}
          "{"]


         [:div.node-key {:class node-key-class
                         :on-mouse-over (u/without-propagation toggle-focused)
                         :on-mouse-out (u/without-propagation toggle-focused)
                         :on-click (u/without-propagation toggle-collapsed)}

          (pr-str k)]

         (cond

           collapsed-group-node? (list [:div.node-brace {:key :opening} "{"]
                                       [:div.node-value.clickable {:key k
                                                                   :class node-value-class
                                                                   :on-mouse-over (u/without-propagation toggle-focused)
                                                                   :on-mouse-out (u/without-propagation toggle-focused)
                                                                   :on-click (u/without-propagation toggle-focused toggle-collapsed)}
                                        "~"]
                                       [:div.node-brace {:key :closing} "}"])

           empty-group-node? (list [:div.node-brace {:key :opening} "{"]
                                   [:div.node-brace {:key :closing} "}"])

           collapsed? [:div.node-value.clickable {:class node-value-class
                                                  :on-mouse-over (u/without-propagation toggle-focused)
                                                  :on-mouse-out (u/without-propagation toggle-focused)
                                                  :on-click (u/without-propagation toggle-collapsed)}
                       "~"]

           node-value? [:div.node-value {:class node-value-class
                                         :on-mouse-over (u/without-propagation toggle-focused)
                                         :on-mouse-out (u/without-propagation toggle-focused)}
                        (pr-str v)]

           last? [node-group φ path {:class node-group-class} {:tail-braces (str tail-braces "}")}]

           :else [node-group φ path {:class node-group-class} {:tail-braces "}"}])

         [:div.node-brace {:class (when-not show-tail-braces? :hidden)}
          tail-braces]]))))


(defn node-group [{:keys [config-opts track-app] :as φ} path _ _]
  (let [log? (get-in config-opts [:tooling :log?])]

    (when log? (log/debug "mount node-group:" path))

    (fn [_ _ props opts]
      (let [{:keys [tail-braces]} opts
            nodes (track-app l/view-single (l/in path))
            num-nodes (count nodes)]

        (when log? (log/debug "render node-group:" path))

        [:div.node-group props
         (doall
          (for [[i [k _]] (map-indexed vector nodes)
                :let [first? (zero? i)
                      last? (= num-nodes (inc i))
                      path (conj path k)]]
            [:div {:key (pr-str k)}
             [node φ path {:tail-braces tail-braces :first? first? :last? last?}]]))]))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top-level components


(defn writes-browser [{:keys [config-opts track-tooling side-effect!] :as φ}]
  (let [log? (get-in config-opts [:tooling :log?])]

    (when log? (log/debug "mount writes-browser"))

    (fn []
      (let [writes (track-tooling l/view (l/*> (l/in [:writes]) l/all-values))
            read-write-index (track-tooling l/view-single (l/in [:read-write-index]))
            track-index (track-tooling l/view-single (l/in [:track-index]))
            real-time? (= track-index read-write-index)
            beginning-of-time? (zero? track-index)]

        (when log? (log/debug "render writes-browser"))

        [:div.writes-browser
         [:div.time-controls
          [:div.time-control.control-play {:class (if real-time? :active :clickable)
                                           :on-click (when-not real-time?
                                                       (u/without-propagation
                                                        #(side-effect! [:tooling/stop-time-travelling])))}
           [:span.icon.icon-control-play]]

          [:div.time-control.control-next {:class (when-not real-time? (u/->class #{:active :clickable}))
                                           :on-click (when-not real-time?
                                                       (u/without-propagation
                                                        #(side-effect! [:tooling/go-forward-in-time])))}
           [:span.icon.icon-control-next]]

          [:div.time-control.control-previous {:class (u/->class (cond-> #{}
                                                                   (not real-time?) (conj :active)
                                                                   (not beginning-of-time?) (conj :clickable)))
                                               :on-click (when (not beginning-of-time?)
                                                           (u/without-propagation
                                                            #(side-effect! [:tooling/go-back-in-time])))}
           [:span.icon.icon-control-prev]]]

         [:div.writes-browser-divider]

         [:div.writes
          (doall
           (for [{:keys [id n]} (take-last track-index (sort-by :n > writes))]
             [:div.write-container {:key n}
              [:div.write-caption
               [:span.write-caption-symbol "Δ"]
               [:span.write-caption-subscript n]]
              [:div.write-shell {:key n}
               [:div.write.writes
                (pr-str id)]]]))]]))))



(defn app-browser [{:keys [config-opts] :as φ}]
  (let [log? (get-in config-opts [:tooling :log?])]

    (when log? (log/debug "mount app-browser"))

    (fn []
      (when log? (log/debug "render app-browser"))
      [:div.browser.app-browser
       [node-group φ [] {} {:tail-braces "}"}]])))



(defn tooling [{:keys [config-opts track-tooling side-effect!] :as φ}]
  (let [log? (get-in config-opts [:tooling :log?])]

    (when log? (log/debug "mount tooling"))

    (fn []
      (let [tooling-active? (track-tooling l/view-single (l/in [:tooling-active?]))]

        (when log? (log/debug "render tooling"))

        [:div.l-overlay.l-overlay--viewport-fixed
         [g/slide-over φ {:open? tooling-active?
                        :absolute-width 800
                        :direction :right}
          [:div.l-overlay__content.c-tooling
           [:div.c-tooling__handle
            {:on-click (u/without-propagation
                        #(side-effect! [:tooling/toggle-tooling-active]))}
            [:div.icon-cog]]
           [:div.l-col.l-height.l-height--full
            [:div.l-col__item.c-tooling__item
             [writes-browser φ]]
            [:div.l-col__item.l-col__item--grow.c-tooling__item
             [app-browser φ]]]]]]))))
