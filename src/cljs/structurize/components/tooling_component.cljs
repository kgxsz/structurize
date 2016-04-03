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
        {:keys [emit-event!]} side-effector
        log? (get-in config-opts [:renderer :tooling :log?])
        !node (r/cursor !db path)
        !node-props (r/cursor !state-browser-props [path])
        toggle-collapse #(emit-event! [:state-browser/toggle-collapsed {:cursor !node-props
                                                                        :hidden-event? true
                                                                        :ignore-throttle? true
                                                                        :Δ (fn [c] (toggle-prop c :collapsed))}])
        toggle-focus #(emit-event! [:state-browser/toggle-focused {:cursor !state-browser-props
                                                                   :hidden-event? true
                                                                   :ignore-throttle? true
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
        log? (get-in config-opts [:renderer :tooling :log?])
        !nodes (r/cursor !db path)]

    (when log? (log/debug "mount node-group:" path))

    (fn [_ _ props opts]
      (let [{:keys [tail-braces] :or {tail-braces "}"}} opts
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


(defn event-browser [{:keys [config-opts state side-effector] :as φ}]
  (let [{:keys [!throttle-events? !throttled-events !processed-events]} state
        {:keys [emit-event! admit-throttled-events!]} side-effector
        log? (get-in config-opts [:renderer :tooling :log?])
        toggle-throttle-events #(emit-event! [:tooling/toggle-throttle-events {:hidden-event? true
                                                                              :ignore-throttle? true
                                                                              :Δ (fn [c] (update-in c [:tooling :throttle-events?] not))}])]

    (when log? (log/debug "mount event-browser"))

    (fn []
      (when log? (log/debug "render event-browser"))
      (let [throttle-events? @!throttle-events?
            throttled-events @!throttled-events
            no-throttled-events? (empty? throttled-events)]
        [:div.browser.event-browser

         [:div.throttle-controls
          [:div.throttle-control.control-play {:class (if throttle-events? :clickable :active)
                                               :on-click (when throttle-events?
                                                           (u/without-propagation admit-throttled-events! toggle-throttle-events))}
           [:span.icon.icon-control-play]]

          [:div.throttle-control.control-pause {:class (if @!throttle-events? :active :clickable)
                                                :on-click (when-not throttle-events? (u/without-propagation toggle-throttle-events))}

           [:span.icon.icon-control-pause]]
          [:div.throttle-control.control-next.clickable {:class (when @!throttle-events? :active)
                                                         :on-click (if throttle-events?
                                                                     (u/without-propagation #(admit-throttled-events! 1))
                                                                     (u/without-propagation toggle-throttle-events))}
           [:span.icon.icon-control-next]]]

         (when throttle-events?
           [:div.throttle-divider])

         (when throttle-events?
           [:div.event-container.throttled-event
            [:div.event-caption
             [:span.event-caption-symbol "ε"]
             [:span.event-caption-subscript "next"]]
            [:div.event-shell
             (if no-throttled-events?
               [:div.event.no-throttled-event
                "no throttled events"]
               [:div.event.throttled-event
                (pr-str (first (last throttled-events)))])]])

         [:div.throttle-divider]

         [:div.processed-events
          (doall
           (for [[id {:keys [emitted-at processed-at n] :as props}] @!processed-events]
             [:div.event-container {:key n}
              [:div.event-caption
               [:span.event-caption-symbol "ε"]
               [:span.event-caption-subscript "n=" n]]
              [:div.event-shell {:key n}
               [:div.event.processed-event
                (pr-str id)]]]))]]))))


(defn state-browser [{:keys [config-opts state side-effector] :as φ}]
  (let [{:keys [emit-event!]} side-effector
        {:keys [!state-browser-props]} state
        log? (get-in config-opts [:renderer :tooling :log?])
        cursor-paths (for [c (vals state) :when (instance? rr/RCursor c)] (.-path c))]

    (when log? (log/debug "mount state-browser"))

    (emit-event! [:state-browser/init-cursored {:cursor !state-browser-props
                                                :hidden-event? true
                                                :ignore-throttle? true
                                                :Δ (fn [state-browser-props]
                                                     (reduce (fn [a v] (update a v add-prop :cursored))
                                                             state-browser-props
                                                             cursor-paths))}])

    (fn []
      (when log? (log/debug "render state-browser"))
      [:div.browser.state-browser
       [node-group φ []]])))


(defn tooling [{:keys [config-opts state side-effector] :as φ}]
  (let [{:keys [emit-event!]} side-effector
        {:keys [!db]} state
        log? (get-in config-opts [:renderer :tooling :log?])
        !tooling-active? (r/cursor !db [:tooling :tooling-active?])
        toggle-tooling-active #(emit-event! [:tooling/toggle-tooling-active {:cursor !tooling-active?
                                                                             :hidden-event? true
                                                                             :ignore-throttle? true
                                                                             :Δ not}])]
    (when log? (log/debug "mount tooling"))
    (fn []
      (let [tooling-active? @!tooling-active?]
        (when log? (log/debug "render tooling"))
        [:div.tooling {:class (when-not tooling-active? :collapsed)}

         [:div.tooling-tab.clickable {:on-click (u/without-propagation toggle-tooling-active)}
          [:span.icon-cog]]

         (when tooling-active?
           [:div.browsers
            [event-browser φ]
            [state-browser φ]])]))))
