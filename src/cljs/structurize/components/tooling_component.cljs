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


(defn node [φ path _]
  (log/debug "mount node:" path)
  (let [{:keys [!core !state-browser-props]} (:state φ)
        {:keys [emit-event!]} (:side-effector φ)
        !node (r/cursor !core path)
        !node-props (r/cursor !state-browser-props [path])
        toggle-collapse #(emit-event! [:state-browser/toggle-collapsed {:cursor !node-props
                                                                        :hidden-event? true
                                                                        :no-throttle? true
                                                                        :Δ (fn [c] (toggle-prop c :collapsed))}])
        toggle-focus #(emit-event! [:state-browser/toggle-focused {:cursor !state-browser-props
                                                                   :hidden-event? true
                                                                   :no-throttle? true
                                                                   :Δ (fn [c]
                                                                        (as-> c c
                                                                          (update c path toggle-prop :focused)
                                                                          (reduce (fn [a v] (update a v toggle-prop :upstream-focused))
                                                                                  c
                                                                                  (-> (reductions conj [] path) rest drop-last))))}])]

    (fn [_ _ opts]
      (log/debug "render node:" path)
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


(defn node-group [φ path _ _]
  (log/debug "mount node-group:" path)
  (let [{:keys [!core]} (:state φ)
        !nodes (r/cursor !core path)]

    (fn [_ _ props opts]
      (log/debug "render node-group:" path)
      (let [{:keys [tail-braces] :or {tail-braces "}"}} opts
            nodes @!nodes
            num-nodes (count nodes)]

        [:div.node-group props
         (doall
          (for [[i [k _]] (map-indexed vector nodes)
                :let [first? (zero? i)
                      last? (= num-nodes (inc i))
                      path (conj path k)]]
            [:div {:key (pr-str k)}
             [node φ path {:tail-braces tail-braces :first? first? :last? last?}]]))]))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; state-browser components




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top-level components


(defn event-browser [φ]
  (log/debug "mount event-browser")
  (let [{:keys [!events]} (:state φ)
        {:keys [admit-pending-event!]} (:side-effector φ)]

    (fn []
      (log/debug "render event-browser")
      [:div.browser.event-browser

       [:div.throttle
        [:div.button.clickable {:on-click (u/without-propagation admit-pending-event!)}
         [:span.button-icon.icon-construction]
         [:span.button-text "throttle"]]]

       [:div.events
        (doall
         (for [[i [id {:keys [emitted-at processed-at n] :as props}]] (map-indexed vector @!events)]
           [:div.event.clickable {:key i}
            (pr-str id) " " n]))]])))


(defn state-browser [φ]
  (log/debug "mount state-browser")
  (let [{:keys [emit-event!]} (:side-effector φ)
        {:keys [!state-browser-props]} (:state φ)
        cursor-paths (for [c (vals (:state φ)) :when (instance? rr/RCursor c)] (.-path c))]

    (emit-event! [:state-browser/init-cursored {:cursor !state-browser-props
                                                :hidden-event? true
                                                :no-throttle? true
                                                :Δ (fn [state-browser-props]
                                                     (reduce (fn [a v] (update a v add-prop :cursored))
                                                             state-browser-props
                                                             cursor-paths))}])

    (fn []
      (log/debug "render state-browser")
      [:div.browser.state-browser
       [node-group φ []]])))


(defn tooling [φ]
  (log/debug "mount tooling")
  (let [{:keys [emit-event!]} (:side-effector φ)
        {:keys [!core]} (:state φ)
        !tooling-active? (r/cursor !core [:tooling :tooling-active?])
        toggle-tooling-active #(emit-event! [:tooling/toggle-tooling-active {:cursor !tooling-active?
                                                                             :hidden-event? true
                                                                             :no-throttle? true
                                                                             :Δ not}])]

    (fn []
      (log/debug "render tooling")
      (let [tooling-active? @!tooling-active?]
        [:div.tooling {:class (when tooling-active? :collapsed)}

         [:div.tooling-tab.clickable {:on-click (u/without-propagation toggle-tooling-active)}
          [:span.icon-cog]]

         (when-not tooling-active?
           [:div.browsers
            [event-browser φ]
            [state-browser φ]])]))))
