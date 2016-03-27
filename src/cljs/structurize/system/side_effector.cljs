(ns structurize.system.side-effector
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defrecord SideEffector [config-opts browser comms machine]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (assoc component
           :emit-event! (:emit-event! machine)
           :admit-pending-event! (:admit-pending-event! machine)
           :send! (:send! comms)
           :post! (:post! comms)
           :change-location! (:change-location! browser)))

  (stop [component] component))
