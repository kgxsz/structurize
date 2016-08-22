(ns structurize.system.config-opts
  (:require [structurize.config :refer [config]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (assoc component
           :tooling {:enabled? true
                     :log? false
                     :max-processed-mutations 50}
           :comms {:chsk-opts {:type :auto
                               :packer :edn}}
           :routes ["/" [["" :home]
                         ["sign-in/github" :sign-in-with-github]
                         [true :unknown]]]))

  (stop [component] component))
