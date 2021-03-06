(ns structurize.system.config-opts
  (:require [structurize.public-config :refer [public-config]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (assoc component
           :tooling {:enabled? true
                     :log? false}
           :viewport {:breakpoints {:xs 544
                                    :sm 768
                                    :md 992
                                    :lg 1200
                                    :xl :infinity}}
           :comms {:chsk-opts {:type :auto
                               :packer :edn}}
           :routes ["/" [["" :home]
                         ["sign-in/github" :sign-in-with-github]
                         [true :unknown]]]))

  (stop [component] component))
