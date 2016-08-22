(ns structurize.components.tooling-component
  (:require [structurize.components.utils :as u]
            [structurize.system.side-effect-bus :refer [side-effect!]]
            [structurize.system.state :refer [track]]
            [structurize.components.general :as g]
            [reagent.core :as r]
            [traversy.lens :as l])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


(declare node-group)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; app-browser components


(defn node [φ path _]
  (let [toggle-collapsed #(side-effect! φ :tooling/toggle-node-collapsed
                                        {:path path})
        toggle-focused #(side-effect! φ :tooling/toggle-node-focused
                                      {:path path})]

    (log-debug φ "mount node:" path)

    (fn [_ _ opts]
      (let [{:keys [tail-braces first? last? downstream-focused?]} opts
            track-index (track φ l/view-single
                               (l/in [:tooling :track-index]))
            node (track φ l/view-single
                        (l/*> (l/in [:app-history track-index]) (l/in path)))
            collapsed? (track φ l/view-single
                              (l/in [:tooling :app-browser-props :collapsed])
                              #(contains? % path))
            written? (track φ l/view-single
                            (l/in [:tooling :app-browser-props :written :paths])
                            #(contains? % path))
            upstream-written? (track φ l/view-single
                                     (l/in [:tooling :app-browser-props :written :upstream-paths])
                                     #(contains? % path))
            focused? (track φ l/view-single
                            (l/in [:tooling :app-browser-props :focused :paths])
                            #(contains? % path))
            upstream-focused? (track φ l/view-single
                                     (l/in [:tooling :app-browser-props :focused :upstream-paths])
                                     #(contains? % path))
            downstream-focused? (or downstream-focused? focused?)

            k (last path)
            v node
            collapsed-group-node? (and collapsed?
                                       (map? v)
                                       (not (empty? node)))
            empty-group-node? (and (map? v) (empty? node))
            node-value? (not (map? v))
            show-tail-braces? (and last? (or collapsed? empty-group-node? node-value?))
            node-key-class (u/->class
                            (cond-> #{}
                              downstream-focused? (conj :c-app-browser__node__key--downstream-focused)
                              first? (conj :c-app-browser__node__key--first)
                              (or focused? upstream-focused?) (conj :c-app-browser__node__key--focused)
                              (or written? upstream-written?) (conj :c-app-browser__node__key--written)))
            node-value-class (u/->class (cond-> #{}
                                          (or focused? downstream-focused?) (conj :c-app-browser__node__value--focused)
                                          (or collapsed-group-node? collapsed?) (conj :c-app-browser__node__value--clickable)
                                          (or written? upstream-written?) (conj :c-app-browser__node__value--written)))]

        (log-debug φ "render node:" path)

        [:div.c-app-browser__node
         [:div.c-app-browser__brace {:class (when-not first? :is-hidden)}
          "{"]


         [:div.c-app-browser__node__key {:class node-key-class
                                         :on-mouse-over (u/without-propagation toggle-focused)
                                         :on-mouse-out (u/without-propagation toggle-focused)
                                         :on-click (u/without-propagation toggle-collapsed)}

          (pr-str k)]

         (cond
           collapsed-group-node? (list [:div.c-app-browser__brace {:key :opening} "{"]
                                       [:div.c-app-browser__node__value {:key k
                                                                         :class node-value-class
                                                                         :on-mouse-over (u/without-propagation toggle-focused)
                                                                         :on-mouse-out (u/without-propagation toggle-focused)
                                                                         :on-click (u/without-propagation toggle-focused toggle-collapsed)}
                                        "~"]
                                       [:div.c-app-browser__brace {:key :closing} "}"])

           empty-group-node? (list [:div.c-app-browser__brace {:key :opening} "{"]
                                   [:div.c-app-browser__brace {:key :closing} "}"])

           collapsed? [:div.c-app-browser__node__value {:class node-value-class
                                                        :on-mouse-over (u/without-propagation toggle-focused)
                                                        :on-mouse-out (u/without-propagation toggle-focused)
                                                        :on-click (u/without-propagation toggle-collapsed)}
                       "~"]

           node-value? [:div.c-app-browser__node__value {:class node-value-class
                                                         :on-mouse-over (u/without-propagation toggle-focused)
                                                         :on-mouse-out (u/without-propagation toggle-focused)}
                        (pr-str v)]

           last? [node-group φ path {:tail-braces (str tail-braces "}") :downstream-focused? downstream-focused?}]

           :else [node-group φ path {:tail-braces "}" :downstream-focused? downstream-focused?}])

         [:div.c-app-browser__brace {:class (when-not show-tail-braces? :is-hidden)}
          tail-braces]]))))


(defn node-group [φ path _]
  (log-debug φ "mount node-group:" path)

  (fn [_ _ opts]
    (let [{:keys [tail-braces downstream-focused?]} opts
          track-index (track φ l/view-single
                             (l/in [:tooling :track-index]))
          nodes (track φ l/view-single
                       (l/*> (l/in [:app-history track-index]) (l/in path)))
          num-nodes (count nodes)]

      (log-debug φ "render node-group:" path)

      [:div
       (doall
        (for [[i [k _]] (map-indexed vector nodes)
              :let [first? (zero? i)
                    last? (= num-nodes (inc i))
                    path (conj path k)]]
          [:div {:key (pr-str k)}
           [node φ path {:tail-braces tail-braces :first? first? :last? last? :downstream-focused? downstream-focused?}]]))])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top-level components


(defn writes-browser [φ]
  (log-debug φ "mount writes-browser")

  (fn []
    (let [writes (track φ l/view
                        (l/*> (l/in [:tooling :writes]) l/all-values))
          read-write-index (track φ l/view-single
                                  (l/in [:tooling :read-write-index]))
          track-index (track φ l/view-single
                             (l/in [:tooling :track-index]))
          real-time? (= track-index read-write-index)
          beginning-of-time? (zero? track-index)]

      (log-debug φ "render writes-browser")

      [:div.l-row.c-writes-browser
       [:div.l-col.c-writes-browser__controls
        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--green
         {:class (if real-time?
                   :c-writes-browser__controls__item--opaque
                   :c-writes-browser__controls__item--clickable)
          :on-click (when-not real-time?
                      (u/without-propagation
                       #(side-effect! φ :tooling/stop-time-travelling)))}
         [:div.c-icon.c-icon--control-play]]

        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--yellow
         {:class (when-not real-time? (u/->class #{:c-writes-browser__controls__item--opaque
                                                   :c-writes-browser__controls__item--clickable}))
          :on-click (when-not real-time?
                      (u/without-propagation
                       #(side-effect! φ :tooling/go-forward-in-time)))}
         [:div.c-icon.c-icon--control-next]]

        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--yellow
         {:class (u/->class (cond-> #{}
                              (not real-time?) (conj :c-writes-browser__controls__item--opaque)
                              (not beginning-of-time?) (conj :c-writes-browser__controls__item--clickable)))
          :on-click (when (not beginning-of-time?)
                      (u/without-propagation
                       #(side-effect! φ :tooling/go-back-in-time)))}
         [:div.c-icon.c-icon--control-prev]]]

       [:div.l-row
        (doall
         (for [{:keys [id n]} (take-last track-index (sort-by :n > writes))]
           [:div.l-col.c-writes-browser__item {:key n}
            [:div.c-writes-browser__pill-superscript
             [:span.c-writes-browser__pill-superscript__symbol "Δ"]
             [:span n]]

            [:div.c-writes-browser__pill
             [:div.c-writes-browser__pill__content
              (pr-str id)]]]))]])))


(defn app-browser [φ]
  (log-debug φ "mount app-browser")

  (fn []
    (log-debug φ "render app-browser")
    [:div.c-app-browser
     [node-group φ [] {:tail-braces "}"}]]))


(defn tooling [φ]
  (let [+slide-over (l/in [:tooling :tooling-slide-over])]

    (log-debug φ "mount tooling")

    (fn []
      [:div.l-overlay.l-overlay--fill-viewport
       [g/slide-over φ {:+slide-over +slide-over
                        :absolute-width 800
                        :direction :right}
        [:div.l-overlay__content.c-tooling
         [:div.c-tooling__handle
          {:on-click (u/without-propagation
                      #(side-effect! φ :tooling/toggle-tooling-slide-over
                                     {:+slide-over +slide-over}))}
          [:div.c-icon.c-icon--cog]]
         [:div.l-col.l-col--fill-parent
          [:div.l-col__item.c-tooling__item
           [writes-browser φ]]
          [:div.l-col__item.l-col__item--grow.c-tooling__item
           [app-browser φ]]]]]])))
