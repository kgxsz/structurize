(ns structurize.system.config-opts
  (:require [structurize.config :refer [config]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defonce general-config-opts
  {:github-auth-url "https://github.com/login/oauth/authorize"
   :init-click-count-a (:init-click-count-a config)})


(defonce chsk-conn-config-opts
  {:chsk-opts {:type :auto}})


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "Initialising config-opts")
    (assoc component
           :general general-config-opts
           :chsk-conn chsk-conn-config-opts))

  (stop [component] component))

