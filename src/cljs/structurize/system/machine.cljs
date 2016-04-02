(ns structurize.system.machine
  (:require [cljs.core.async :as a]
            [cljs-time.core :as t]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defn decorate-event [config-opts [id props] events]
  (let [max-past-events (get-in config-opts [:machine :max-past-events])
        n (or (-> events first second :n) 0)
        event [id (assoc (select-keys props [:emitted-at])
                         :processed-at (t/now)
                         :n (inc n))]]
    (take max-past-events (cons event events))))


(defn process-event

  "This function operates on state with the mutating functions provided in the events themselves.

   id - the id of the event
   cursor - if included, will operate on the cursor into the state, if not, will operate on the db
   Δ - a function that takes the db or the cursor and produces the desired change in state."

  [config-opts event state]

  (let [[id {:keys [cursor Δ hidden-event?]}] event
        {:keys [!db !processed-events]} state]

    (log/debug "processing event:" id)
    (if-let [cursor-or-db (and Δ (or cursor !db))]
      (do
        (swap! cursor-or-db Δ)
        (when-not hidden-event?
          (swap! !processed-events (partial decorate-event config-opts event))))
      (log/error "failed to process event:" id))))


(defn throttle-event [[id _ :as event] {:keys [!throttled-events]}]
  (log/debug "throttling event:" id)
  (swap! !throttled-events conj event))


(defn make-emit-event
  "Returns a function that emits events onto the event channel."
  [<event]
  (fn [[id props]]
    (log/debug "emitting event:" id)
    (let [event [id (assoc props :emitted-at (t/now))]]
      (go (a/>! <event event)))))


(defn make-admit-throttled-events
  "Returns a function that puts onto the admit throttled events channel.
   If n is defined, then up to n throttled events will be admitted, if n
   is not defined, then all throttled events will be admitted."
  [<admit-throttled-events]
  (fn [n]
    (go (a/>! <admit-throttled-events (or n :all)))))


(defn listen-for-emit-event [config-opts <event {:keys [!throttle-events?] :as state}]
  (go-loop [[id {:keys [ignore-throttle?]} :as event] (a/<! <event)]
    (if (and @!throttle-events? (not ignore-throttle?))
      (throttle-event event state)
      (process-event config-opts event state))
    (recur (a/<! <event))))


 (defn listen-for-admit-throttled-events [config-opts <admit-throttled-events {:keys [!throttled-events] :as state}]
   (go-loop [n (a/<! <admit-throttled-events)]
     (let [events @!throttled-events
           n-events (count events)]
       (if (zero? n-events)
         (log/debug "no throttled events to admit")
         (let [n (if (integer? n) n n-events)]
           (log/debugf "admitting %s throttled event(s)" n)
           (swap! !throttled-events (partial drop-last n))
           (doseq [event (reverse (take-last n events))] (process-event config-opts event state)))))
     (recur (a/<! <admit-throttled-events))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Machine [config-opts state]
  component/Lifecycle

  (start [component]
    (log/info "initialising machine")
    (let [<event (a/chan)
          <admit-throttled-events (a/chan)]

      (log/info "begin listening for emitted events")
      (listen-for-emit-event config-opts <event state)

      (log/info "begin listening for admittance of throttled events")
      (listen-for-admit-throttled-events config-opts <admit-throttled-events state)

      (assoc component
             :emit-event! (make-emit-event <event)
             :admit-throttled-events! (make-admit-throttled-events <admit-throttled-events))))

  (stop [component] component))
