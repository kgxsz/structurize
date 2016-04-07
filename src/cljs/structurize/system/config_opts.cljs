(ns structurize.system.config-opts
  (:require [structurize.config :refer [config]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (assoc component
           :host (:host config)
           :tooling {:enabled? true
                     :log? false
                     :show-in-state-browser? false
                     :max-processed-mutations 50}
           :comms {:chsk-opts {:type :auto
                               :packer :edn}}))

  (stop [component] component))

