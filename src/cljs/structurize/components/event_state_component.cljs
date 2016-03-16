(ns structurize.components.event-state-component
  (:require [taoensso.timbre :as log]
            [reagent.ratom :as rr]
            [clojure.string :as string]))

;; TODO rename to tooling-component
;; TODO use cursors to update only the things that need updating


(declare nodes)


(defn stringify [x]
  (cond
    (nil? x) "nil"
    (string? x) (str "\"" x "\"")
    :else (str x)))


(defn toggle-property [s property]
  (cond
    (contains? s property) (disj s property)
    s (conj s property)
    :else #{property}))


(defn node [φ core node-path braces]
  (let [k (last node-path)
        v (get-in core node-path)
        node-properties (get-in core [:tooling :node-properties node-path])
        trail-nodes-paths (-> (reductions conj [] node-path) rest drop-last)
        !node-properties (get-in φ [:state :!node-properties])
        emit-event! (get-in φ [:side-effector :emit-event!])
        toggle-node-focus (fn [] (emit-event! [:focus-node {:cursor !node-properties
                                                       :Δ (fn [node-properties]
                                                            (reduce
                                                             (fn [a v] (update a v toggle-property :trail-focused))
                                                             (update node-properties node-path toggle-property :node-focused)
                                                             trail-nodes-paths))}]))]

    [:div.node {:key k}
     [:div.node-key-container
      [:div.node-key
       {:class (when (some #{:node-focused :trail-focused} node-properties) :focused)
        :on-mouse-over (fn [e] (toggle-node-focus) (.stopPropagation e))
        :on-mouse-out (fn [e] (toggle-node-focus) (.stopPropagation e))}

       [:div.node-key-flags
        [:div.node-key-flag {:class (when (contains? node-properties :cursored) :cursored)}]]

       (stringify k)]]

     (if (map? v)
       [nodes φ core node-path (str braces "}")]
       [:div.node-value
        {:class (when (contains? node-properties :node-focused) :focused)
         :on-mouse-over (fn [e] (toggle-node-focus) (.stopPropagation e))
         :on-mouse-out (fn [e] (toggle-node-focus) (.stopPropagation e))}
        (stringify v)])

     (when-not (map? v) [:div braces])]))


(defn nodes [φ core nodes-path braces]
  (let [node-paths (map (partial conj nodes-path) (keys (get-in core nodes-path)))
        node-properties (get-in core [:tooling :node-properties nodes-path])]

    [:div.nodes-container {:class (when (contains? node-properties :node-focused) :focused)}
     [:div (str "{" (when (empty? node-paths) braces))]
     [:nodes
      (for [node-path (drop-last node-paths)]
        (node φ core node-path ""))
      (when (seq node-paths)
        (node φ core (last node-paths) braces))]]))


(defn state-display [φ]

  (log/debug "mount state-display")

  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        !core (get-in φ [:state :!core])
        !node-properties (get-in φ [:state :!node-properties])
        cursor-paths (for [c (-> φ :state vals) :when (instance? rr/RCursor c)] (.-path c))]

    (emit-event! [:setup-node-properties {:cursor !node-properties
                                          :Δ (fn [node-properties]
                                               (reduce (fn [a v] (update a v toggle-property :cursored))
                                                node-properties
                                                cursor-paths))}])

    (fn []
      (log/debug "render state-display")
      [:div.state-display
       (nodes φ @!core [] "}")])))


(defn event-state-display [φ]
  (log/debug "mount/render event-state")
  [:div.event-state-display
   [state-display φ]])

