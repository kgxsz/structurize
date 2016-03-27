(ns structurize.system.side-effector
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defrecord SideEffector [config-opts browser comms machine]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (assoc component
           :emit-event! (:emit-event! machine)
           :admit-throttled-event! (:admit-throttled-event! machine)
           :flush-throttled-events! (:flush-throttled-events! machine)
           :send! (:send! comms)
           :post! (:post! comms)
           :change-location! (:change-location! browser)))

  (stop [component] component))
