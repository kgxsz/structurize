(ns structurize.system.machine
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(defn process-event [[id {:keys [cursor Δ]}] {:keys [!core] :as state}]
  (log/debug "processing event:" id)
  (if-let [cursor-or-core (and Δ (or (get state cursor) !core))]
    (do (swap! !core assoc :event id)
        (swap! cursor-or-core Δ))
    (log/error "Failed to process event:" id)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Machine [config-opts state bus]
  component/Lifecycle

  (start [component]
    (log/info "Initialising machine")
    (let [<event (:<event bus)]
      (go-loop [] (process-event (a/<! <event) state) (recur))
      component))

  (stop [component] component))
