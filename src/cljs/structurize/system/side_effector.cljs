(ns structurize.system.side-effector
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defrecord SideEffector [config-opts browser comms state]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (assoc component
           :emit-mutation! (get-in state [:mutators :emit-mutation!])
           :admit-throttled-mutations! (get-in state [:mutators :admit-throttled-mutations!])
           :send! (:send! comms)
           :post! (:post! comms)
           :change-location! (:change-location! browser)))

  (stop [component] component))
