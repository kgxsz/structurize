(ns structurize.components.even-state-component
  (:require [taoensso.timbre :as log]))

(defn event-state [{:keys [state]}]
  (log/debug "mount/render event-state")
  (let [core @(:!core state)]
    [:div.event-state
     [:div.state
      [:div.opening-brace "{"]
      [:div.keys-values
       (for [[k v] core]
         [:div.key-value {:key k}
          (str k " " v)])]]]))
