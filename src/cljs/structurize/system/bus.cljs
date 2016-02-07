(ns structurize.system.bus
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defrecord Bus [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising bus")
    (let [<event (a/chan)]
      (assoc component :<event <event)))

  (stop [component] component))
