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


(defn add-property [s property]
  (if s
    (conj s property)
    #{property}))


(defn remove-property [s property]
  (if s
    (disj s property)
    #{}))


(defn toggle-property [s property]
  (if (contains? s property)
    (remove-property s property)
    (add-property s property)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component helpers


(defn node [φ core node-path braces]
  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        k (last node-path)
        v (get-in core node-path)
        node-properties (get-in core [:tooling :node-properties node-path])
        focused? (= (take (count node-path) (get-in core [:tooling :focused-node])) node-path)
        toggle-node-collapsed (fn [] (emit-event! [:toggle-collapses {:cursor (get-in φ [:state :!node-properties])
                                                                :Δ (fn [node-properties]
                                                                     (update node-properties node-path toggle-property :node-collapsed))}]))
        set-focused-node (fn [] (emit-event! [:set-focused-node {:Δ (fn [core]
                                                                     (assoc-in core [:tooling :focused-node] node-path))}]))
        clear-focused-node (fn [] (emit-event! [:clear-focused-node {:Δ (fn [core]
                                                                         (assoc-in core [:tooling :focused-node] nil))}]))]

    [:div.node {:key k}
     [:div.node-key-container
      [:div.node-key {:class (when focused? :focused)
                      :on-mouse-over (fn [e] (set-focused-node) (.stopPropagation e))
                      :on-mouse-out (fn [e] (clear-focused-node) (.stopPropagation e))
                      :on-click (fn [e] (toggle-node-collapsed) (.stopPropagation e))}

       [:div.node-key-flags
        (when (contains? node-properties :cursored)
          [:div.node-key-flag.cursored [:span.icon.icon-pushpin]])
        #_(when (contains? node-properties :cursored)
          [:div.node-key-flag.mutated [:span.icon.icon-star]])]

       (stringify k)]]

     (if (map? v)
       [nodes φ core node-path (str braces "}")]
       [:div.node-value {:class (when focused? :focused)
                         :on-mouse-over (fn [e] (set-focused-node) (.stopPropagation e))
                         :on-mouse-out (fn [e] (clear-focused-node) (.stopPropagation e))}

        (if (contains? node-properties :node-collapsed)
          [:div.collapsed-value [:div] [:div] [:div]]
          (stringify v))])

     (when-not (map? v) [:div.braces braces])]))


(defn nodes [φ core nodes-path braces]
  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        node-paths (map (partial conj nodes-path) (keys (get-in core nodes-path)))
        node-properties (get-in core [:tooling :node-properties nodes-path])
        node-collapsed? (and (seq node-paths) (contains? node-properties :node-collapsed))
        focused? (= nodes-path (get-in core [:tooling :focused-node]))
        toggle-node-collapsed (fn [] (emit-event! [:toggle-collapses {:cursor (get-in φ [:state :!node-properties])
                                                                 :Δ (fn [node-properties]
                                                                      (update node-properties nodes-path toggle-property :node-collapsed))}]))
        set-focused-node (fn [] (emit-event! [:set-focused-node {:Δ (fn [core]
                                                                     (assoc-in core [:tooling :focused-node] nodes-path))}]))
        clear-focused-node (fn [] (emit-event! [:clear-focused-node {:Δ (fn [core]
                                                                         (assoc-in core [:tooling :focused-node] nil))}]))]

    (if node-collapsed?
      [:div.nodes-container {:class (when focused? :focused)
                             :on-mouse-over (fn [e] (set-focused-node) (.stopPropagation e))
                             :on-mouse-out (fn [e] (clear-focused-node) (.stopPropagation e))}
       [:div.braces "{"]
       [:div.node-value [:div.collapsed-value [:div] [:div] [:div]]]
       [:div.braces braces]]
      [:div.nodes-container {:class (when focused? :focused)}
       [:div.braces (str "{" (when (empty? node-paths) braces))]
       [:nodes
        (for [node-path (drop-last node-paths)]
          (node φ core node-path ""))
        (when-not (empty? node-paths)
          (node φ core (last node-paths) braces))]])))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; components


(defn state-browser [φ]

  (log/debug "mount state-browser")

  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        !core (get-in φ [:state :!core])
        !node-properties (get-in φ [:state :!node-properties])
        cursor-paths (for [c (-> φ :state vals) :when (instance? rr/RCursor c)] (.-path c))]

    (emit-event! [:setup-cursored-nodes {:cursor !node-properties
                                         :Δ (fn [node-properties]
                                              (reduce (fn [a v] (update a v add-property :cursored))
                                                      node-properties
                                                      cursor-paths))}])

    (fn []
      (log/debug "render state-browser")
      [:div.state-browser
       (nodes φ @!core [] "}")])))


(defn tooling [φ]
  (log/debug "mount/render tooling")
  (let [emit-event! (get-in φ [:side-effector :emit-event!])
        !tooling (r/cursor (get-in φ [:state :!core]) [:tooling])
        tooling-collapsed? (:tooling-collapsed? @!tooling)
        toggle-tooling-collapsed #(emit-event! [:toggle-tooling-collapsed {:cursor !tooling
                                                                           :Δ (fn [tooling] (update tooling :tooling-collapsed? not))}])]

    [:div.tooling {:class (when tooling-collapsed? :collapsed)}

     [:div.tooling-tab {:on-click (fn [e] (toggle-tooling-collapsed) (.stopPropagation e))}
      [:span.icon-cog]]

     (when-not tooling-collapsed?
       [:div.browsers
        [state-browser φ]
        [:div.event-browser]])]))
