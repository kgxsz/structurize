(ns structurize.system.machine
  (:require [cljs.core.async :as a]
            [cljs-time.core :as t]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defn decorate-event [[id props] events]
  (let [n (or (-> events first second :n) 0)
        event [id (assoc (select-keys props [:emitted-at])
                         :processed-at (t/now)
                         :n (inc n))]]
    (take 10 (cons event events))))


(defn process-event
  "This function is the only thing that can operate on state.

   id - the id of the event
   cursor - if included, will operate on the cursor into the state, if not, will operate on the core
   Δ - a function that takes the core or the cursor and produces the desired change in state."
  [event state]

  (let [[id {:keys [cursor Δ hidden-event?]}] event
        {:keys [!core !processed-events]} state]

    (log/debug "processing event:" id)
    (if-let [cursor-or-core (and Δ (or cursor !core))]
      (do
        (swap! cursor-or-core Δ)
        (when-not hidden-event?
          (swap! !processed-events (partial decorate-event event))))
      (log/error "failed to process event:" id))))


(defn throttle-event [<throttled-event event state]
  (let [[id _] event
        {:keys [!throttled-events]} state]
    (log/debug "throttling event:" id)
    (go (a/>! <throttled-event event))
    (swap! !throttled-events conj id)))


(defn make-emit-event
  "Returns a function that emits events onto the event channel."
  [<event]
  (fn [[id props]]
    (log/debug "emitting event:" id)
    (let [event [id (assoc props :emitted-at (t/now))]]
      (go (a/>! <event event)))))


(defn make-admit-throttled-event
  "Returns a function that puts a trigger onto the admit throttled event channel."
  [<admit-throttled-event]
  (fn []
    (log/debug "attempting to admit throttled event")
    (go (a/>! <admit-throttled-event :trigger))))


(defn make-flush-throttled-events
  "Returns a function that puts a trigger onto the flush throttled events channel."
  [<flush-throttled-events]
  (fn []
    (log/debug "attempting to flush throttled events")
    (go (a/>! <flush-throttled-events :trigger))))


(defn listen-for-emit-event [<event <throttled-event {:keys [!throttle-events?] :as state}]
  (go-loop [[id {:keys [ignore-throttle?]} :as event] (a/<! <event)]
    (if (and @!throttle-events? (not ignore-throttle?))
      (throttle-event <throttled-event event state)
      (process-event event state))
    (recur (a/<! <event))))


(defn listen-for-admit-throttled-event [<admit-throttled-event <throttled-event state]
  (let [{:keys [!throttled-events]} state]
    (go-loop [_ (a/<! <admit-throttled-event)]
      (when (seq @!throttled-events)
        (swap! !throttled-events drop-last)
        (process-event (a/<! <throttled-event) state))
      (recur (a/<! <admit-throttled-event)))))


(defn listen-for-flush-throttled-events [<flush-throttled-events <throttled-event state]
  (let [{:keys [!throttled-events]} state]
    (go-loop [_ (a/<! <flush-throttled-events)]
      (doseq [_ @!throttled-events]
        (swap! !throttled-events drop-last)
        (process-event (a/<! <throttled-event) state))
      (recur (a/<! <flush-throttled-events)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Machine [config-opts state]
  component/Lifecycle

  (start [component]
    (log/info "initialising machine")
    (let [<event (a/chan)
          <admit-throttled-event (a/chan)
          <flush-throttled-events (a/chan)
          <throttled-event (a/chan)]

      (log/info "begin listening for emitted events")
      (listen-for-emit-event <event <throttled-event state)

      (log/info "begin listening for admittance of throttled events")
      (listen-for-admit-throttled-event <admit-throttled-event <throttled-event state)

      (log/info "begin listening for throttled events flushing")
      (listen-for-flush-throttled-events <flush-throttled-events <throttled-event state)

      (assoc component
             :emit-event! (make-emit-event <event)
             :admit-throttled-event! (make-admit-throttled-event <admit-throttled-event)
             :flush-throttled-events! (make-flush-throttled-events <flush-throttled-events))))

  (stop [component] component))
