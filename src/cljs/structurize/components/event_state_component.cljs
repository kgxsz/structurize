(ns structurize.components.event-state-component
  (:require [taoensso.timbre :as log]
            [reagent.ratom :as rr]
            [clojure.string :as string]))


(defn stringify [x]
  (cond
    (nil? x) "nil"
    (string? x) (str "\"" x "\"")
    :else (str x)))


(defn classify [& classes]
  (->> classes
       (remove nil?)
       (map name)
       (interpose " ")
       (apply str)))


(defn state-nodes
  [{{:keys [emit-event!]} :side-effector {:keys [!core]} :state :as φ} node cursors path close-brace-depth]

  (let [num-keys (count node)
        focused-node (get-in @!core [:tooling :focused-node])]

    [:div.nodes-container {:class (when (= path focused-node) :focused)}

     [:div.nodes-braces (str "{" (when (empty? node) (apply str (repeat close-brace-depth "}"))))]

     [:div.nodes

      (map-indexed
       (fn [i [k v]]
         (let [close-brace? (= (inc i) num-keys)
               path (conj path k)
               focus-node #(emit-event! [:focus-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] path))}])
               blur-node #(emit-event! [:blur-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] nil))}])]
           [:div.node {:key k}
            [:div.node-key-container
             [:div.node-key
              {:class (when (= path (take (count path) focused-node)) :focused)
               :on-mouse-over (fn [e] (focus-node) (.stopPropagation e))
               :on-mouse-out (fn [e] (blur-node) (.stopPropagation e))}

              [:div.node-key-flag {:class (when (contains? cursors path) :cursored)}]

              (stringify k)]]
            (if (map? v)
              [state-nodes φ v cursors path (if close-brace? (inc close-brace-depth) close-brace-depth)]
              [:div.node-value
               {:class (when (= path focused-node) :focused)
                :on-mouse-over (fn [e] (focus-node) (.stopPropagation e))
                :on-mouse-out (fn [e] (blur-node) (.stopPropagation e))}
               (stringify v)])
            (when (and close-brace? (not (map? v)))
              [:div.node-braces (apply str (repeat close-brace-depth "}"))])]))
       node)]]))


(defn state-root-nodes
  [{:keys [state] :as φ}]
  (let [node @(:!core state)
        cursors (->> (vals state)
                     (filter (partial instance? rr/RCursor))
                     (map #(.-path %))
                     set)]
    [state-nodes φ node cursors [] 1]))


(defn event-state [φ]
  (log/debug "mount/render event-state")
  [:div.event-state
   [:div.state
    [state-root-nodes φ]]])

