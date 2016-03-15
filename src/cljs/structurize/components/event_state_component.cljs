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


(defn node [φ k v cursors focused-node focus-node blur-node path braces]
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
     [nodes φ path (str braces "}")]
     [:div.node-value
      {:class (when (= path focused-node) :focused)
       :on-mouse-over (fn [e] (focus-node) (.stopPropagation e))
       :on-mouse-out (fn [e] (blur-node) (.stopPropagation e))}
      (stringify v)])

   (when-not (map? v) [:div.node-braces braces])])


(defn nodes [φ path braces]

  (log/debug "mount/render nodes:" path)

  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        core @(get-in φ [:state :!core])
        current-nodes (get-in core path)
        focused-node (get-in core [:tooling :focused-node])]

    [:div.nodes-container {:class (when (= path focused-node) :focused)}
     [:div.nodes-braces "{"]

     (if (empty? current-nodes)
       [:div.nodes-braces braces]

       [:nodes

        (for [[k v] (drop-last current-nodes)]
          (let [braces ""
                path (conj path k)
                cursors (get-in core [:tooling :cursors])
                focused-node (get-in core [:tooling :focused-node])
                focus-node #(emit-event! [:focus-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] path))}])
                blur-node #(emit-event! [:blur-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] nil))}])]

            (node φ k v cursors focused-node focus-node blur-node path braces)))

        (let [[k v] (last current-nodes)
              path (conj path k)
              cursors (get-in core [:tooling :cursors])
              focused-node (get-in core [:tooling :focused-node])
              focus-node #(emit-event! [:focus-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] path))}])
              blur-node #(emit-event! [:blur-node {:Δ (fn [core] (assoc-in core [:tooling :focused-node] nil))}])]

          (node φ k v cursors focused-node focus-node blur-node path braces))])]))


(defn root-nodes [φ]

  (log/debug "mount/render root-nodes")

  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        cursor-paths (for [c (-> φ :state vals) :when (instance? rr/RCursor c)] (.-path c))
        cursors (reduce #(assoc-in %1 %2 :cursored) {} cursor-paths)
        path []
        braces "}"]

    (emit-event! [:setup-tooling-cursors {:Δ (fn [core] (assoc-in core [:tooling :cursors] cursors))}])

    [nodes φ path braces]))


(defn event-state [φ]
  (log/debug "mount/render event-state")
  [:div.event-state
   [:div.state
    [root-nodes φ]]])

