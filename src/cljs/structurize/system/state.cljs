(ns structurize.system.state
  (:require [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [cljs-time.core :as t]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))



(defn make-update-processed-mutations [config-opts mutation]
  (fn [mutations]
    (let [max-processed-mutations (get-in config-opts [:state :max-processed-mutations])
          n (or (-> mutations first second :n) 0)
          [id props] mutation
          props (assoc (select-keys props [:emitted-at])
                       :processed-at (t/now)
                       :n (inc n))
          mutation [id props]]
      (take max-processed-mutations (cons mutation mutations)))))


(defn process-mutation

  "This function operates on state with the mutating functions provided in the mutations themselves.

   id - the id of the mutation
   cursor - if included, will operate on the cursor into the state, if not, will operate on the db
   Δ - a function that takes the db or the cursor and produces the desired change in state."

  [config-opts mutation !db]

  (let [[id {:keys [cursor Δ tooling?]}] mutation
        log? (or (not tooling?) (get-in config-opts [:general :tooling :log?]))]

    (when log? (log/debug "processing mutation:" id))
    (if-let [cursor-or-db (and Δ (or cursor !db))]
      (do
        (swap! cursor-or-db Δ)
        (when-not tooling?
          (swap! !db update-in [:tooling :processed-mutations] (make-update-processed-mutations config-opts mutation))))
      (log/error "failed to process mutation:" id))))


(defn throttle-mutation [[id _ :as mutation] !db]
  (log/debug "throttling mutation:" id)
  (swap! !db update-in [:tooling :throttled-mutations] conj mutation))


(defn make-emit-mutation
  "Returns a function that emits a mutation onto the mutation channel."
  [config-opts <mutation]
  (fn [[id {:keys [tooling?] :as props}]]
    (let [mutation [id (assoc props :emitted-at (t/now))]
          log? (or (not tooling?) (get-in config-opts [:general :tooling :log?]))]
      (when log? (log/debug "emitting mutation:" id))
      (go (a/>! <mutation mutation)))))


(defn make-admit-throttled-mutations

  "Returns a function that puts onto the admit throttled mutations channel.
   If n is defined, then up to n throttled mutations will be admitted, if n
   is not defined, then all throttled mutations will be admitted."

  [<admit-throttled-mutations]

  (fn [n]
    (go (a/>! <admit-throttled-mutations (or n :all)))))


(defn listen-for-emit-mutation [config-opts !db <mutation]
  (go-loop []
    (let [[id {:keys [tooling?]} :as mutation] (a/<! <mutation)
          throttle-mutations? (get-in @!db [:tooling :throttle-mutations?])]
      (if (and throttle-mutations? (not tooling?))
        (throttle-mutation mutation !db)
        (process-mutation config-opts mutation !db)))
    (recur)))


 (defn listen-for-admit-throttled-mutations [config-opts !db <admit-throttled-mutations]
   (go-loop []
     (let [n (a/<! <admit-throttled-mutations)
           throttled-mutations (get-in @!db [:tooling :throttled-mutations])
           n-mutations (count throttled-mutations)]
       (if (zero? n-mutations)
         (log/debug "no throttled mutations to admit")
         (let [n (if (integer? n) n n-mutations)]
           (log/debugf "admitting %s throttled mutation(s)" n)
           (swap! !db update-in [:tooling :throttled-mutations] (partial drop-last n))
           (doseq [mutation (reverse (take-last n throttled-mutations))] (process-mutation config-opts mutation !db)))))
     (recur)))



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
          <admit-throttled-mutations (a/chan)]

      (log/info "begin listening for emitted mutations")
      (listen-for-emit-mutation config-opts !db <mutations)

      (log/info "begin listening for admittance of throttled mutations")
      (listen-for-admit-throttled-mutations config-opts !db <admit-throttled-mutations)

      (assoc component
             :!db !db
             :mutators {:emit-mutation! (make-emit-mutation config-opts <mutations)
                        :admit-throttled-mutations! (make-admit-throttled-mutations <admit-throttled-mutations)}
             :!handler (r/cursor !db [:location :handler])
             :!query (r/cursor !db [:location :query])
             :!chsk-status (r/cursor !db [:comms :chsk-status])
             :!throttle-mutations? (r/cursor !db [:tooling :throttle-mutations?])
             :!throttled-mutations (r/cursor !db [:tooling :throttled-mutations])
             :!processed-mutations (r/cursor !db [:tooling :processed-mutations])
             :!state-browser-props (r/cursor !db [:tooling :state-browser-props]))))

  (stop [component] component))
