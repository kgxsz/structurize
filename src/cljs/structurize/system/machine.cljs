(ns structurize.system.machine
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(defn process-event [[id {:keys [cursor Δ]}] state]
  (log/debug "processing event:" id)
  (if-let [state (and Δ (get state (or cursor :!global)))]
    (swap! state Δ)
    (log/error "Failed to process event:" id)))


(defrecord Machine [config-opts state bus]
  component/Lifecycle

  (start [component]
    (log/info "Initialising machine")
    (let [<event (:<event bus)]
      (go-loop [] (process-event (a/<! <event) state) (recur))
      component))

  (stop [component] component))


