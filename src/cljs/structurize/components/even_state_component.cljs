(ns structurize.components.even-state-component
  (:require [taoensso.timbre :as log]
            [clojure.string :as string]))


(defn render-as-string [x]
  (cond
    (nil? x) [:span "nil"]
    (string? x) [:span (str "\"" x "\"")]
    :else [:span (str x)]))


(defn state-node [node close-depth]
  (let [num-keys (count node)]

    [:div.node
     [:div "{" (when (empty? node) (apply str (repeat close-depth "}")))]
     [:div.keys-values
      (map-indexed
       (fn [i [k v]]
         (let [close? (= (inc i) num-keys)]
           [:div.key-value {:key k
                            :class (when close? :close)}
            [:div.key (render-as-string k)]
            (if (map? v)
              [state-node v (if close? (inc close-depth) close-depth)]
              [:div.value (render-as-string v)])
            (when (and close? (not (map? v)))
              [:div.closing-brace (apply str (repeat close-depth "}"))])]))
       node)]]))


(defn event-state [{:keys [state]}]
  (log/debug "mount/render event-state")
  [:div.event-state
   [:div.state
    [state-node @(:!core state) 1]]])

