(ns structurize.system.machine
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(defn process-event

  "This function is the only thing that can operate on state.

   id - the id of the event
   cursor - if included, will operate on the cursor into the state, if not, will operate on the core
   Δ - a function that takes the core or the cursor and produces the desired change in state."

  [event state]

  (let [[id {:keys [cursor Δ priviledged?]}] event
        {:keys [!core !events]} state]

    (log/debug "processing event:" id)
    (if-let [cursor-or-core (and Δ (or cursor !core))]
      (do
        (when-not priviledged?
          (swap! !events (comp (partial cons {:id id}) (partial take 3))))
        (swap! cursor-or-core Δ))
      (log/error "failed to process event:" id))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Machine [config-opts state bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising machine")
    (let [<event (:<event bus)]
      (go-loop [] (process-event (a/<! <event) state) (recur))
      component))

  (stop [component] component))
