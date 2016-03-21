(ns structurize.components.tooling-component
  (:require [taoensso.timbre :as log]
            [reagent.ratom :as rr]
            [reagent.core :as r]
            [clojure.string :as string]))

(declare nodes)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; util


(defn stringify [x]
  (cond
    (nil? x) "nil"
    (string? x) (str "\"" x "\"")
    :else (str x)))


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


(defn make-toggle-prop [prop]
  (fn [s]
    (toggle-prop s prop)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component helpers


(defn node [φ node-path braces]
  (log/debug "mount node:" node-path)
  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        {:keys [!core !focused-node]} (get-in φ [:state])
        !node-props (r/cursor !core [:tooling :nodes-props node-path])
        !node-value (r/cursor !core node-path)
        lead-node-paths (-> (reductions conj [] node-path) rest drop-last)
        toggle-node-collapsed #(emit-event! [:toggle-collapsed {:cursor !node-props
                                                                :Δ (make-toggle-prop :node-collapsed)}])
        toggle-node-focus #(emit-event! [:toggle-node-focus {:cursor (get-in φ [:state :!nodes-props])
                                                             :Δ (fn [nodes-props]
                                                                  (reduce (fn [a v] (update a v toggle-prop :lead-focused))
                                                                          (update nodes-props node-path toggle-prop :focused)
                                                                          lead-node-paths))}])]

    (fn []
      (log/debug "render node:" node-path)
      (let [node-props @!node-props
            v @!node-value]
        [:div.node
         [:div.node-key-container
          [:div.node-key {:class (when (some #{:focused :lead-focused} node-props) :focused)
                          :on-mouse-over (fn [e] (toggle-node-focus) (.stopPropagation e))
                          :on-mouse-out (fn [e] (toggle-node-focus) (.stopPropagation e))
                          :on-click (fn [e] (toggle-node-collapsed) (.stopPropagation e))}

           [:div.node-key-flags
            (when (contains? node-props :cursored)
              [:div.node-key-flag.cursored [:span.icon.icon-pushpin]])
            #_(when (contains? node-properties :cursored)
                [:div.node-key-flag.mutated [:span.icon.icon-star]])]

           (stringify (last node-path))]]

         (if (map? v)
           [nodes φ node-path (str braces "}")]
           [:div.node-value {:class (when (contains? node-props :focused) :focused)
                             :on-mouse-over (fn [e] (toggle-node-focus) (.stopPropagation e))
                             :on-mouse-out (fn [e] (toggle-node-focus) (.stopPropagation e))}

            (if (contains? node-props :node-collapsed)
              [:div.collapsed-value [:div] [:div] [:div]]
              (stringify v))])

         (when-not (map? v) [:div.braces braces])]))))


(defn nodes [φ nodes-path braces]
  (log/debug "mount nodes:" nodes-path)
  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        {:keys [!core !nodes-props !focused-node]} (get-in φ [:state])
        !node-props (r/cursor !core [:tooling :nodes-props nodes-path])
        !nodes (r/cursor !core nodes-path)
        lead-node-paths (-> (reductions conj [] nodes-path) rest drop-last)
        toggle-node-collapsed #(emit-event! [:toggle-collapsed {:cursor !nodes-props
                                                                :Δ (make-toggle-prop :node-collapsed)}])
        toggle-node-focus #(emit-event! [:toggle-node-focus {:cursor (r/cursor !core [:tooling :nodes-props])
                                                             :Δ (fn [nodes-props]
                                                                  (reduce (fn [a v] (update a v toggle-prop :lead-focused))
                                                                          (update nodes-props nodes-path toggle-prop :focused)
                                                                          lead-node-paths))}])]

    (fn []
      (log/debug "render nodes:" nodes-path)
      (let [node-props @!node-props
            nodes @!nodes
            node-paths (map (partial conj nodes-path) (keys nodes))
            nodes-collapsed? (and (seq node-paths) (contains? node-props :node-collapsed))
            focused? (contains? node-props :focused)]
        (if nodes-collapsed?
          [:div.nodes-container {:class (when focused? :focused)
                                 :on-mouse-over (fn [e] (toggle-node-focus) (.stopPropagation e))
                                 :on-mouse-out (fn [e] (toggle-node-focus) (.stopPropagation e))}
           [:div.braces "{"]
           [:div.node-value [:div.collapsed-value [:div] [:div] [:div]]]
           [:div.braces braces]]
          [:div.nodes-container {:class (when focused? :focused)}
           [:div.braces (str "{" (when (empty? node-paths) braces))]
           [:nodes
            (doall
             (for [node-path (drop-last node-paths)]
               [:div.node-container {:key (str node-path)}
                [node φ node-path ""]]))
            (when-let [node-path (last node-paths)]
              [:div.node-container {:key (str node-path)}
               [node φ (last node-paths) braces]])]])))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; components


(defn state-browser [φ]

  (log/debug "mount state-browser")

  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        cursor-paths (for [c (-> φ :state vals) :when (instance? rr/RCursor c)] (.-path c))]

    (emit-event! [:setup-cursored-nodes {:cursor (get-in φ [:state :!nodes-props])
                                         :Δ (fn [nodes-props]
                                              (reduce (fn [a v] (update a v add-prop :cursored))
                                                      nodes-props
                                                      cursor-paths))}])

    (fn []
      (log/debug "render state-browser")
      [:div.state-browser
       [nodes φ [] "}"]])))


(defn tooling [φ]
  (log/debug "mount tooling")
  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        !tooling-collapsed? (r/cursor (get-in φ [:state :!core]) [:tooling :tooling-collapsed?])
        toggle-tooling-collapsed #(emit-event! [:toggle-tooling-collapsed {:cursor !tooling-collapsed?
                                                                           :Δ not}])]

    (fn []
      (log/debug "render tooling")
      (let [tooling-collapsed? @!tooling-collapsed?]
        [:div.tooling {:class (when tooling-collapsed? :collapsed)}

         [:div.tooling-tab {:on-click (fn [e] (toggle-tooling-collapsed) (.stopPropagation e))}
          [:span.icon-cog]]

         (when-not tooling-collapsed?
           [:div.browsers
            [state-browser φ]
            [:div.event-browser]])]))))
