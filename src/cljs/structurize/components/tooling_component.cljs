(ns structurize.components.tooling-component
  (:require [structurize.components.component-utils :as u]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

(declare node-group)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; state-browser components


(defn node [{:keys [config-opts state emit-side-effect!] :as φ} path _]
  (let [{:keys [!db !state-browser-props]} state
        log? (get-in config-opts [:tooling :log?])
        !node (r/cursor !db path)
        toggle-collapsed #(emit-side-effect! [:tooling/toggle-node-collapsed {:path path}])
        toggle-focused #(emit-side-effect! [:tooling/toggle-node-focused {:path path}])]

    (when log? (log/debug "mount node:" path))

    (fn [_ _ opts]
      (let [{:keys [tail-braces first? last?]} opts
            node @!node
            {:keys [cursored mutated collapsed focused]} @!state-browser-props
            collapsed? (contains? collapsed path)
            cursored? (contains? (:paths cursored) path)
            upstream-cursored? (contains? (:upstream-paths cursored) path)
            mutated? (contains? (:paths mutated) path)
            upstream-mutated? (contains? (:upstream-paths mutated) path)
            focused?  (contains? (:paths focused) path)
            upstream-focused? (contains? (:upstream-paths focused) path)
            k (last path)
            v @!node
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
                              cursored? (conj :cursored)
                              upstream-cursored? (conj :upstream-cursored)
                              mutated? (conj :mutated)
                              upstream-mutated? (conj :upstream-mutated)
                              first? (conj :first)))
            node-value-class (u/->class (cond-> #{}
                                          focused? (conj :focused)
                                          mutated? (conj :mutated)
                                          upstream-mutated? (conj :upstream-mutated)
                                          upstream-cursored? (conj :upstream-cursored)))]

        (when log? (log/debug "render node:" path))

        [:div.node
         [:div.node-brace {:class (when-not first? :hidden)}
          "{"]


         [:div.node-key {:class node-key-class
                         :on-mouse-over (fn [e] (toggle-focused) (.stopPropagation e))
                         :on-mouse-out (fn [e] (toggle-focused) (.stopPropagation e))
                         :on-click (fn [e] (toggle-collapsed) (.stopPropagation e))}

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

           last? [node-group φ path {:class node-value-class} {:tail-braces (str tail-braces "}")}]

           :else [node-group φ path {:class node-value-class} {:tail-braces "}"}])

         [:div.node-brace {:class (when-not show-tail-braces? :hidden)}
          tail-braces]]))))


(defn node-group [{:keys [config-opts state] :as φ} path _ _]
  (let [{:keys [!db]} state
        log? (get-in config-opts [:tooling :log?])
        !nodes (r/cursor !db path)]

    (when log? (log/debug "mount node-group:" path))

    (fn [_ _ props opts]
      (let [{:keys [tail-braces]} opts
            nodes @!nodes
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


(defn mutation-browser [{:keys [config-opts state emit-side-effect!] :as φ}]
  (let [{:keys [!db]} state
        !processed-mutations (r/cursor !db [:tooling :processed-mutations])
        !unprocessed-mutations (r/cursor !db [:tooling :unprocessed-mutations])
        log? (get-in config-opts [:tooling :log?])]

    (when log? (log/debug "mount mutation-browser"))

    (fn []
      (let [processed-mutations @!processed-mutations
            unprocessed-mutations @!unprocessed-mutations
            real-time? (empty? unprocessed-mutations)
            beginning-of-time? (empty? processed-mutations)]

        (when log? (log/debug "render mutation-browser"))

        [:div.browser.mutation-browser
         [:div.time-controls
          [:div.time-control.control-play {:class (when real-time? :active)}
           [:span.icon.icon-control-play]]

          [:div.time-control.control-previous {:class (u/->class (cond-> #{}
                                                                   (not real-time?) (conj :active)
                                                                   (not beginning-of-time?) (conj :clickable)))
                                               :on-click (when-not beginning-of-time?
                                                           (u/without-propagation
                                                            #(emit-side-effect! [:tooling/back-in-time])))}
           [:span.icon.icon-control-prev]]

          [:div.time-control.control-next {:class (when-not real-time? (u/->class #{:active :clickable}))
                                           :on-click (when-not real-time?
                                                       (u/without-propagation
                                                        #(emit-side-effect! [:tooling/forward-in-time])))}
           [:span.icon.icon-control-next]]]

         #_(when time-mutations?
             [:div.time-divider])

         #_(when time-mutations?
           [:div.mutation-container.timed-mutation
            [:div.mutation-caption
             [:span.mutation-caption-symbol "Δ"]
             [:span.mutation-caption-subscript "next"]]
            [:div.mutation-shell
             (if no-timed-mutations?
               [:div.mutation.no-timed-mutation
                "no timed mutations"]
               [:div.mutation.timed-mutation
                (pr-str (first (last timed-mutations)))])]])

         [:div.mutation-browser-divider]

         [:div.processed-mutations
          (doall
           (for [[id {:keys [emitted-at processed-at n] :as props}] processed-mutations]
             [:div.mutation-container {:key n}
              [:div.mutation-caption
               [:span.mutation-caption-symbol "Δ"]
               [:span.mutation-caption-subscript n]]
              [:div.mutation-shell {:key n}
               [:div.mutation.processed-mutation
                (pr-str id)]]]))]]))))



(defn state-browser [{:keys [config-opts state emit-side-effect!] :as φ}]
  (let [log? (get-in config-opts [:tooling :log?])]

    (when log? (log/debug "mount state-browser"))

    (fn []
      (when log? (log/debug "render state-browser"))
      [:div.browser.state-browser
       [node-group φ [] {:tail-braces "}"}]])))


(defn cursor-browser [{:keys [config-opts state emit-side-effect!] :as φ}]
  (let [{:keys [!cursors]} state
        log? (get-in config-opts [:tooling :log?])
        toggle-cursored (fn [path] (emit-side-effect! [:tooling/toggle-node-cursored {:path path}]))]

    (when log? (log/debug "mount cursor-browser"))
    (emit-side-effect! [:tooling/cursor-browser-init])

    (fn []
      (let [cursors @!cursors]
        (when log? (log/debug "render cursor-browser"))
        [:div.browser.cursor-browser
         (for [[key path] cursors]
           [:div.cursor.clickable {:key key
                                   :on-mouse-over (u/without-propagation #(toggle-cursored path))
                                   :on-mouse-out (u/without-propagation #(toggle-cursored path))}
            (pr-str key)])]))))


(defn tooling [{:keys [config-opts state emit-side-effect!] :as φ}]
  (let [{:keys [!db]} state
        log? (get-in config-opts [:tooling :log?])
        !tooling-active? (r/cursor !db [:tooling :tooling-active?])]

    (when log? (log/debug "mount tooling"))

    (fn []
      (let [tooling-active? @!tooling-active?]

        (when log? (log/debug "render tooling"))

        [:div.tooling {:class (when-not tooling-active? :collapsed)}
         [:div.tooling-tab.clickable {:on-click (u/without-propagation
                                                 #(emit-side-effect! [:tooling/toggle-tooling-active]))}
          [:span.icon-cog]]

         (when tooling-active?
           [:div.browsers
            [mutation-browser φ]
            [state-browser φ]
            [cursor-browser φ]])]))))
