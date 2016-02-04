(ns structurize.system.bus
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defrecord Bus [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Initialising bus")
    (let [<event (a/chan)
          emit-event! (fn [e] (go (a/>! <event e)))]
      (assoc component :<event <event :emit-event! emit-event!)))

  (stop [component] component))
