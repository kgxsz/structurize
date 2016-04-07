(ns structurize.system.config-opts
  (:require [structurize.config :refer [config]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defonce general-config-opts
  {:host (:host config)
   :tooling {:log? false}})

(defonce browser-config-opts
  {})

(defonce comms-config-opts
  {:chsk-opts {:type :auto
               :packer :edn}})

(defonce side-effector-config-opts
  {})

(defonce state-config-opts
  {:max-processed-mutations 30})

(defonce renderer-config-opts
  {:tooling {:enabled? true
             :show-in-state-browser? true}})



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (assoc component
           :general general-config-opts
           :browser browser-config-opts
           :comms comms-config-opts
           :side-effector side-effector-config-opts
           :state state-config-opts
           :renderer renderer-config-opts))

  (stop [component] component))

