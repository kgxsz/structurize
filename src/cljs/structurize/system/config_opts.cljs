(ns structurize.system.config-opts
  (:require [structurize.config :refer [config]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defonce general-config-opts
  {:github-auth-url "https://github.com/login/oauth/authorize"
   :init-click-count-a (:init-click-count-a config)})


(defonce bus-config-opts
  {})

(defonce comms-config-opts
  {:chsk-opts {:type :auto}})


(defonce state-config-opts
  {})


(defonce machine-config-opts
  {})


(defonce renderer-config-opts
  {})


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "Initialising config-opts")
    (assoc component
           :general general-config-opts
           :bus bus-config-opts
           :comms comms-config-opts
           :state state-config-opts
           :machine machine-config-opts
           :renderer renderer-config-opts))

  (stop [component] component))

