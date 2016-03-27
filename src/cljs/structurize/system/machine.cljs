(ns structurize.system.machine
  (:require [cljs.core.async :as a]
            [cljs-time.core :as t]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; 1) You decorate the event if and only if tooling is active?
;; 2) You can throttle the event if it's not priveledged


(defn make-record-event [[id props]]
  (fn [events]
    (let [n (or (-> events first second :n) 0)
          event [id (assoc (select-keys props [:emitted-at])
                           :processed-at (t/now)
                           :n (inc n))]]
      (take 10 (cons event events)))))


(defn process-event

  "This function is the only thing that can operate on state.

   id - the id of the event
   cursor - if included, will operate on the cursor into the state, if not, will operate on the core
   Δ - a function that takes the core or the cursor and produces the desired change in state."

  [event state]

  (let [[id {:keys [cursor Δ hidden-event?]}] event
        {:keys [!core !events]} state]

    (log/debug "processing event:" id)
    (if-let [cursor-or-core (and Δ (or cursor !core))]
      (do
        (swap! cursor-or-core Δ)
        (when-not hidden-event? (swap! !events (make-record-event event))))
      (log/error "failed to process event:" id))))


(defn make-emit-event!
  "Returns a function that emits events onto the bus' event channel."
  [<event]
  (fn [[id props]]
    (log/debug "emitting event:" id)
    (let [event [id (assoc props :emitted-at (t/now))]]
      (go (a/>! <event event)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Machine [config-opts state]
  component/Lifecycle

  (start [component]
    (log/info "initialising machine")
    (let [<event (a/chan)
          <throttle (a/chan)
          <pending-event (a/chan)]
      (go-loop [] (process-event (a/<! <event) state) (recur))
      (assoc component
             :admit-pending-event! #(println "dayum!")
             :emit-event! (make-emit-event! <event))))

  (stop [component] component))
