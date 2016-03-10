(ns structurize.system.side-effector
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defrecord SideEffector [config-opts bus browser comms]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (assoc component
           :emit-event! (:emit-event! bus)
           :send! (:send! comms)
           :post! (:post! comms)
           :change-history! (:change-history! browser)))

  (stop [component] component))
