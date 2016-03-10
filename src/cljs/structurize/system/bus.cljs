(ns structurize.system.bus
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; event emitter setup


(defn make-emit-event!
  "Returns a function that emits events onto the bus' event channel."
  [<event]
  (fn [[id _ :as event]]
    (log/debug "emitting event:" id)
    (go (a/>! <event event))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Bus [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising bus")
    (let [<event (a/chan)]
      (assoc component
             :<event <event
             :emit-event! (make-emit-event! <event))))

  (stop [component] component))
