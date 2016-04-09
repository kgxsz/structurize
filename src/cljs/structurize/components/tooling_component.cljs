(ns structurize.components.tooling-component
  (:require [structurize.components.component-utils :as u]
            [cljs-time.core :as t]
            [reagent.core :as r]
            [reagent.ratom :as rr]
            [taoensso.timbre :as log]))

(declare node-group)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; helpers


(defn add-prop [s prop]
  (if s
    (conj s prop)
    #{prop}))


(defn remove-prop [s prop]
  (if s
    (disj s prop)
    #{}))


(defn toggle-prop [s prop]
  (if (contains? s prop)
    (remove-prop s prop)
    (add-prop s prop)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; state-browser components


(defn node [{:keys [config-opts state side-effector] :as φ} path _]
  (let [{:keys [!db !state-browser-props]} state
        {:keys [emit-mutation!]} side-effector
        log? (get-in config-opts [:tooling :log?])
        !node (r/cursor !db path)
        !node-props (r/cursor !state-browser-props [path])
        toggle-collapse #(emit-mutation! [:state-browser/toggle-collapsed {:cursor !node-props
                                                                        :tooling? true
                                                                        :Δ (fn [c] (toggle-prop c :collapsed))}])
        toggle-focus #(emit-mutation! [:state-browser/toggle-focused {:cursor !state-browser-props
                                                                   :tooling? true
                                                                   :Δ (fn [c]
                                                                        (as-> c c
                                                                          (update c path toggle-prop :focused)
                                                                          (reduce (fn [a v] (update a v toggle-prop :upstream-focused))
                                                                                  c
                                                                                  (-> (reductions conj [] path) rest drop-last))))}])]

    (when log? (log/debug "mount node:" path))

    (fn [_ _ opts]
      (let [{:keys [tail-braces first? last?]} opts
            node @!node
            node-props @!node-props
            k (last path)
            v @!node
            upstream-focused? (contains? node-props :upstream-focused)
            focused? (contains? node-props :focused)
            collapsed? (contains? node-props :collapsed)
            collapsed-group-node? (and collapsed?
                                       (map? v)
                                       (not (empty? node)))
            empty-group-node? (and (map? v) (empty? node))
            node-value? (not (map? v))
            show-tail-braces? (and last? (or collapsed? empty-group-node? node-value?))]

        (when log? (log/debug "render node:" path))

        [:div.node
         [:div.node-brace {:class (when-not first? :hidden)}
          "{"]


         [:div.node-key {:class (u/->class
                                 (cond-> #{:clickable}
                                   focused? (conj :focused)
                                   upstream-focused? (conj :upstream-focused)
                                   first? (conj :first)))
                         :on-mouse-over (fn [e] (toggle-focus) (.stopPropagation e))
                         :on-mouse-out (fn [e] (toggle-focus) (.stopPropagation e))
                         :on-click (fn [e] (toggle-collapse) (.stopPropagation e))}

          (when (contains? node-props :cursored)
            [:div.node-key-flag.cursored [:span.icon.icon-pushpin]])
          (when (contains? node-props :mutated)
            [:div.node-key-flag.mutated [:span.icon.icon-star]])

          (pr-str k)]

         (cond

           collapsed-group-node? (list [:div.node-brace {:key :opening} "{"]
                                       [:div.node-value.clickable {:key k
                                                                   :class (when focused? :focused)
                                                                   :on-mouse-over (u/without-propagation toggle-focus)
                                                                   :on-mouse-out (u/without-propagation toggle-focus)
                                                                   :on-click (u/without-propagation toggle-focus toggle-collapse)}
                                        "~"]
                                       [:div.node-brace {:key :closing} "}"])

           empty-group-node? (list [:div.node-brace {:key :opening} "{"]
                                   [:div.node-brace {:key :closing} "}"])

           collapsed? [:div.node-value.clickable {:class (when focused? :focused)
                                                  :on-mouse-over (u/without-propagation toggle-focus)
                                                  :on-mouse-out (u/without-propagation toggle-focus)
                                                  :on-click (u/without-propagation toggle-collapse)}
                       "~"]

           node-value? [:div.node-value {:class (when focused? :focused)
                                         :on-mouse-over (u/without-propagation toggle-focus)
                                         :on-mouse-out (u/without-propagation toggle-focus)}
                        (pr-str v)]

           last? [node-group φ path (when focused? {:class :focused}) {:tail-braces (str tail-braces "}")}]

           :else [node-group φ path (when focused? {:class :focused})])

         [:div.node-brace {:class (when-not show-tail-braces? :hidden)}
          tail-braces]]))))


(defn node-group [{:keys [config-opts state] :as φ} path _ _]
  (let [{:keys [!db]} state
        show-tooling? (get-in config-opts [:tooling :show-in-state-browser?])
        log? (get-in config-opts [:tooling :log?])
        !nodes (r/cursor !db path)]

    (when log? (log/debug "mount node-group:" path))

    (fn [_ _ props opts]
      (let [{:keys [tail-braces] :or {tail-braces "}"}} opts
            nodes (cond->> @!nodes
                    (not show-tooling?) (remove (fn [[k _]] (= :tooling k))))
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


(defn mutation-browser [{:keys [config-opts state side-effector] :as φ}]
  (let [{:keys [!throttle-mutations? !throttled-mutations !processed-mutations]} state
        {:keys [emit-mutation! admit-throttled-mutations!]} side-effector
        log? (get-in config-opts [:tooling :log?])
        toggle-throttle-mutations #(emit-mutation! [:tooling/toggle-throttle-mutations {:cursor !throttle-mutations?
                                                                               :tooling? true
                                                                               :Δ not}])]

    (when log? (log/debug "mount mutation-browser"))

    (fn []
      (when log? (log/debug "render mutation-browser"))
      (let [throttle-mutations? @!throttle-mutations?
            throttled-mutations @!throttled-mutations
            no-throttled-mutations? (empty? throttled-mutations)]
        [:div.browser.mutation-browser

         [:div.throttle-controls
          [:div.throttle-control.control-play {:class (if throttle-mutations? :clickable :active)
                                               :on-click (when throttle-mutations?
                                                           (u/without-propagation admit-throttled-mutations! toggle-throttle-mutations))}
           [:span.icon.icon-control-play]]

          [:div.throttle-control.control-pause {:class (if @!throttle-mutations? :active :clickable)
                                                :on-click (when-not throttle-mutations? (u/without-propagation toggle-throttle-mutations))}

           [:span.icon.icon-control-pause]]
          [:div.throttle-control.control-next.clickable {:class (when @!throttle-mutations? :active)
                                                         :on-click (if throttle-mutations?
                                                                     (u/without-propagation #(admit-throttled-mutations! 1))
                                                                     (u/without-propagation toggle-throttle-mutations))}
           [:span.icon.icon-control-next]]]

         (when throttle-mutations?
           [:div.throttle-divider])

         (when throttle-mutations?
           [:div.mutation-container.throttled-mutation
            [:div.mutation-caption
             [:span.mutation-caption-symbol "Δ"]
             [:span.mutation-caption-subscript "next"]]
            [:div.mutation-shell
             (if no-throttled-mutations?
               [:div.mutation.no-throttled-mutation
                "no throttled mutations"]
               [:div.mutation.throttled-mutation
                (pr-str (first (last throttled-mutations)))])]])

         [:div.throttle-divider]

         [:div.processed-mutations
          (doall
           (for [[id {:keys [emitted-at processed-at n] :as props}] @!processed-mutations]
             [:div.mutation-container {:key n}
              [:div.mutation-caption
               [:span.mutation-caption-symbol "Δ"]
               [:span.mutation-caption-subscript n]]
              [:div.mutation-shell {:key n}
               [:div.mutation.processed-mutation
                (pr-str id)]]]))]]))))


(defn state-browser [{:keys [config-opts state side-effector] :as φ}]
  (let [{:keys [emit-mutation!]} side-effector
        {:keys [!state-browser-props]} state
        log? (get-in config-opts [:tooling :log?])
        cursor-paths (for [c (vals state) :when (instance? rr/RCursor c)] (.-path c))]

    (when log? (log/debug "mount state-browser"))

    (emit-mutation! [:state-browser/init-cursored {:cursor !state-browser-props
                                                :tooling? true
                                                :Δ (fn [state-browser-props]
                                                     (reduce (fn [a v] (update a v add-prop :cursored))
                                                             state-browser-props
                                                             cursor-paths))}])

    (fn []
      (when log? (log/debug "render state-browser"))
      [:div.browser.state-browser
       [node-group φ []]])))


(defn tooling [{:keys [config-opts state side-effector] :as φ}]
  (let [{:keys [emit-side-effect! emit-mutation!]} side-effector
        {:keys [!db]} state
        log? (get-in config-opts [:tooling :log?])
        !tooling-active? (r/cursor !db [:tooling :tooling-active?])
        toggle-tooling-active #(emit-side-effect! [:tooling/toggle-tooling-active])]

    (when log? (log/debug "mount tooling"))
    (fn []
      (let [tooling-active? @!tooling-active?]
        (when log? (log/debug "render tooling"))
        [:div.tooling {:class (when-not tooling-active? :collapsed)}

         [:div.tooling-tab.clickable {:on-click (u/without-propagation toggle-tooling-active)}
          [:span.icon-cog]]

         (when tooling-active?
           [:div.browsers
            [mutation-browser φ]
            [state-browser φ]])]))))
