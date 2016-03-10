(ns structurize.components.even-state-component
  (:require [taoensso.timbre :as log]))

(defn event-state [{:keys [state]}]
  (log/debug "mount/render event-state")
  (let [core @(:!core state)]
    [:div.event-state
     [:div.state
      (for [[k v] core]
        [:div {:key k}
         (str k " " v)])]]))
