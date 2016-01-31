(ns structurize.system.machine
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(defn dispatch-event [[type data] state chsk-conn]
  (log/debug "Dispatching event:" type)
  (case type
    :inc-click-count/a (swap! (:!click-count-a state) inc)
    :inc-click-count/b (swap! (:!click-count-b state) inc)
    (log/error "No dispatch for event:" type)))


(defrecord Machine [config-opts <event chsk-conn state]
  component/Lifecycle

  (start [component]
    (log/info "Initialising machine")
    (let [<loop (go-loop [] (dispatch-event (a/<! <event) state chsk-conn) (recur))]
      (assoc component :<loop <loop)))

  (stop [component] component))


