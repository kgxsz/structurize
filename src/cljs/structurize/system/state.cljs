(ns structurize.system.state
  (:require [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [cljs-time.core :as t]
            [cljs.core.async :as a]
            [clojure.data :as d]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defn hydrate-mutation
  "Adds a few useful things to the mutation's properties."
  [!db mutation]
  (let [latest-mutation (first (get-in @!db [:tooling :processed-mutations]))
        [id props] mutation
        hydrated-props (-> props
                           (dissoc :Δ)
                           (assoc :processed (t/now)
                                  :n (inc (:n (second latest-mutation) 0))))]
    [id hydrated-props]))


(defn make-update-processed-mutations

  "Returns a function that updates the processed-mutations by adding the new one,
   and dropping the oldest one."

  [config-opts !db]

  (let [max-processed-mutations (get-in config-opts [:tooling :max-processed-mutations])]
    (fn [processed-mutations hydrated-mutation]
      (take max-processed-mutations (cons hydrated-mutation processed-mutations)))))


(defn make-process-tooling-mutation

  "Returns a function that operates on state with the mutating function (Δ) provided in the mutation.
   However, it expects tooling mutations only, so they are treated differently than regular mutations."

  [config-opts !db]

  (let [log? (get-in config-opts [:general :tooling :log?])]

    (fn [mutation]
      (let [[id {:keys [cursor Δ]}] mutation]

        (when log? (log/debug "processing tooling mutation:" id))

        (if-let [cursor-or-db (and Δ (or cursor !db))]
          (do
            (swap! cursor-or-db Δ))
          (log/error "failed to process tooling mutation:" id))))))


(defn make-process-mutation
  "Returns a function that operates on state with the mutating function (Δ) provided in the mutation."
  [config-opts !db]

  (let [update-processed-mutations (make-update-processed-mutations config-opts !db)]

    (fn [mutation]
      (let [[id {:keys [cursor Δ]}] mutation
            hydrated-mutation (hydrate-mutation !db mutation)
            db @!db]

        (log/debug "processing mutation:" id)

        (if-let [cursor-or-db (and Δ (or cursor !db))]
          (do
            (swap! cursor-or-db Δ)
            #_ (log/warn (d/diff @!db db)) ;; [things-only-in-a things-only-in-b things-in-both]
            (swap! !db update-in [:tooling :processed-mutations] update-processed-mutations hydrated-mutation))
          (log/error "failed to process mutation:" id))))))


(defn throttle-mutation [[id _ :as mutation] !db]
  (log/debug "throttling mutation:" id)
  (swap! !db update-in [:tooling :throttled-mutations] conj mutation))


(defn make-emit-mutation

  "Returns a function that emits a mutation onto the mutation channel.

   mutations are vectors made up of the following:
   id - the id of the mutation
   cursor - if included, will operate on the cursor into the state, if not, will operate on the db
   Δ - a function that takes the db or the cursor and produces the desired change in state. "

  [config-opts <mutation]

  (fn [[id props]]
    (let [mutation [id (assoc props :emitted-at (t/now))]]
      (go (a/>! <mutation mutation)))))


(defn make-admit-throttled-mutations

  "Returns a function that puts onto the admit throttled mutations channel.
   If n is defined, then up to n throttled mutations will be admitted, if n
   is not defined, then all throttled mutations will be admitted."

  [<admit-throttled-mutations]
  (fn [n]
    (go (a/>! <admit-throttled-mutations (or n :all)))))


(defn listen-for-emit-mutation

  "Listens for activity on the <mutation channel, upon which the received
   mutation will be either processed as a tooling mutation, throttled,
   or processed normally."

  [config-opts !db <mutation]

  (let [process-mutation (make-process-mutation config-opts !db)
        process-tooling-mutation (make-process-tooling-mutation config-opts !db)]

    (go-loop []
      (let [[id props :as mutation] (a/<! <mutation)
            tooling? (= (namespace id) "tooling")
            throttle-mutations? (get-in @!db [:tooling :throttle-mutations?])]
        (cond
          tooling? (process-tooling-mutation mutation)
          throttle-mutations? (throttle-mutation mutation !db)
          :else (process-mutation mutation)))
      (recur))))


(defn listen-for-admit-throttled-mutations

  "Listens for activity on the <admit-throttled-mutation channel, upon which
   n or all throttled mutations will be processed."

  [config-opts !db <admit-throttled-mutations]

   (let [process-mutation (make-process-mutation config-opts !db)]

     (go-loop []
       (let [n (a/<! <admit-throttled-mutations)
             throttled-mutations (get-in @!db [:tooling :throttled-mutations])
             n-mutations (count throttled-mutations)]

         (if (zero? n-mutations)
           (log/debug "no throttled mutations to admit")

           (let [n (if (integer? n) n n-mutations)]
             (log/debugf "admitting %s throttled mutation(s)" n)
             (swap! !db update-in [:tooling :throttled-mutations] (partial drop-last n))
             (doseq [mutation (reverse (take-last n throttled-mutations))] (process-mutation mutation)))))

       (recur))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; db setup


(defn make-db [config-opts]
  (r/atom {:playground {:heart 0
                        :star 3}
           :location {:path nil
                      :handler :unknown-page
                      :query nil}
           :comms {:chsk-status :init
                   :message {}
                   :post {}}
           :tooling {:tooling-active? true
                     :throttle-mutations? false
                     :throttled-mutations '()
                     :processed-mutations '()
                     :state-browser-props {[:tooling :processed-mutations] #{:collapsed}
                                           [:tooling :throttled-mutations] #{:collapsed}
                                           [:tooling :state-browser-props] #{:collapsed}}}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising state")
    (let [!db (make-db config-opts)
          <mutations (a/chan)
          <admit-throttled-mutations (a/chan)
          process-tooling-mutation (make-process-tooling-mutation config-opts !db)]

      (log/info "begin listening for emitted mutations")
      (listen-for-emit-mutation config-opts !db <mutations)

      (log/info "begin listening for admittance of throttled mutations")
      (listen-for-admit-throttled-mutations config-opts !db <admit-throttled-mutations)

      (assoc component
             :!db !db

             :emit-mutation! (make-emit-mutation config-opts <mutations)
             :admit-throttled-mutations! (make-admit-throttled-mutations <admit-throttled-mutations)

             :!handler (r/cursor !db [:location :handler])
             :!query (r/cursor !db [:location :query])
             :!chsk-status (r/cursor !db [:comms :chsk-status])
             :!throttle-mutations? (r/cursor !db [:tooling :throttle-mutations?])
             :!throttled-mutations (r/cursor !db [:tooling :throttled-mutations])
             :!processed-mutations (r/cursor !db [:tooling :processed-mutations])
             :!state-browser-props (r/cursor !db [:tooling :state-browser-props]))))

  (stop [component] component))
