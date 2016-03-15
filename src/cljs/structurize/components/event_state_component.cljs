(ns structurize.components.event-state-component
  (:require [taoensso.timbre :as log]
            [reagent.ratom :as rr]
            [clojure.string :as string]))


(defn stringify [x]
  (cond
    (nil? x) "nil"
    (string? x) (str "\"" x "\"")
    :else (str x)))


(declare nodes)


(defn node [core current-node emit-event! path braces]
  (let [[k v] current-node
        path (conj path k)
        cursors (get-in core [:tooling :cursors])
        focused-node (get-in core [:tooling :focused-node])
        focus-node #(emit-event! [:focus-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] path))}])
        blur-node #(emit-event! [:blur-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] nil))}])]

    [:div.node {:key k}
     [:div.node-key-container
      [:div.node-key
       {:class (when (= path (take (count path) focused-node)) :focused)
        :on-mouse-over (fn [e] (focus-node) (.stopPropagation e))
        :on-mouse-out (fn [e] (blur-node) (.stopPropagation e))}

       [:div.node-key-flags
        [:div.node-key-flag {:class (when (get-in cursors path) :cursored)}]]

       (stringify k)]]

     (if (map? v)
       [nodes core emit-event! path (str braces "}")]
       [:div.node-value
        {:class (when (= path focused-node) :focused)
         :on-mouse-over (fn [e] (focus-node) (.stopPropagation e))
         :on-mouse-out (fn [e] (blur-node) (.stopPropagation e))}
        (stringify v)])

     (when-not (map? v) [:div braces])]))


(defn nodes [core emit-event! path braces]
  (let [current-nodes (get-in core path)
        focused-node (get-in core [:tooling :focused-node])]

    [:div.nodes-container {:class (when (= path focused-node) :focused)}
     [:div "{"]
     (if (empty? current-nodes)
       [:div braces]
       [:nodes
        (for [current-node (drop-last current-nodes)]
          (node core current-node emit-event! path ""))
        (node core (last current-nodes) emit-event! path braces)])]))


(defn state-display [φ]

  (log/debug "mount state-display")

  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        !core (get-in φ [:state :!core])
        cursor-paths (for [c (-> φ :state vals) :when (instance? rr/RCursor c)] (.-path c))
        cursors (reduce #(assoc-in %1 %2 :cursored) {} cursor-paths)]

    (emit-event! [:setup-tooling-cursors {:Δ (fn [core] (assoc-in core [:tooling :cursors] cursors))}])

    (fn []
      (log/debug "render state-display")
      [:div.state-display
       (nodes @!core emit-event! [] "}")])))


(defn event-state [φ]
  (log/debug "mount/render event-state")
  [:div.event-state-display
   [state-display φ]])

