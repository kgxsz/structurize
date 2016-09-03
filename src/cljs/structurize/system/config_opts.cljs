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
           :viewport {:breakpoints {:xs 544
                                    :sm 768
                                    :md 992
                                    :lg 1200
                                    :xl :infinity}
                      :grid {:gutter {:xs 6
                                      :sm 6
                                      :md 10
                                      :lg 10
                                      :xl 10}
                             :max-col-width 220
                             :min-col-width 190
                             :min-col-n 2}}
           :comms {:chsk-opts {:type :auto
                               :packer :edn}}
           :routes ["/" [["" :home]
                         ["sign-in/github" :sign-in-with-github]
                         [true :unknown]]]))

  (stop [component] component))
