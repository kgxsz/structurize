(ns structurize.system.state
  (:require [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [cljs-time.core :as t]
            [cljs.core.async :as a]
            [clojure.data :as d]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; cheeky helpers


(defn map-paths [m]
  (if (or (not (map? m))
          (empty? m))
    '(())
    (for [[k v] m
          subkey (map-paths v)]
      (cons k subkey))))


(defn build-diff [added removed]
  (let [paths (set (concat (map-paths added) (map-paths removed)))]
    (reduce
     (fn [a path] (assoc a path {:before (get-in removed path) :after (get-in added path)}))
     {}
     paths)))


(defn upstream-paths [paths]
  (->> paths
       (map drop-last)
       (remove empty?)
       (map (partial reductions conj []))
       (map rest)
       (apply concat)
       set))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; cheeky helpers


(defn hydrate-mutation
  "Adds a few useful things to the mutation's properties."
  [db-before db-after mutation]

  (let [previous-mutation (first (get-in db-before [:tooling :processed-mutations]))
        [added removed _] (d/diff db-after db-before)
        mutation-paths (into #{} (map-paths added))
        upstream-mutation-paths (upstream-paths mutation-paths)
        diff (build-diff added removed)
        [id props] mutation
        hydrated-props (-> props
                           (dissoc :Δ)
                           (assoc :processed (t/now)
                                  :n (inc (:n (second previous-mutation) 0))
                                  :diff diff
                                  :mutation-paths mutation-paths
                                  :upstream-mutation-paths upstream-mutation-paths))]
    (log/warn added)
    (log/warn removed)
    (log/warn mutation-paths)
    (log/warn diff)
    [id hydrated-props]))


(defn make-update-tooling

  [{:keys [config-opts] :as Φ}]
  (let [max-processed-mutations (get-in config-opts [:tooling :max-processed-mutations])]

    (fn [db hydrated-mutation]
      (let [[_ {:keys [mutation-paths upstream-mutation-paths]}] hydrated-mutation]
        (-> db
            (update-in [:tooling :processed-mutations] (comp (partial take max-processed-mutations) (partial cons hydrated-mutation)))
            (assoc-in [:tooling :state-browser-props :mutated :paths] mutation-paths)
            (assoc-in [:tooling :state-browser-props :mutated :upstream-paths] upstream-mutation-paths))))))


(defn make-process-tooling-mutation

  "Returns a function that operates on state with the mutating function (Δ) provided in the mutation.
   However, it expects tooling mutations only, so they are treated differently than regular mutations."

  [{:keys [config-opts !db] :as Φ}]

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
  [{:keys [config-opts !db] :as Φ}]

  (let [update-tooling (make-update-tooling Φ)
        tooling-enabled? (get-in config-opts [:tooling :enabled?])]

    (fn [mutation]
      (let [[id {:keys [cursor Δ]}] mutation
            real-time? (empty? (get-in @!db [:tooling :unprocessed-mutations]))]

        (if real-time?

          (do
            (log/debug "processing mutation:" id)

            (if-let [cursor-or-db (and Δ (or cursor !db))]

              (if tooling-enabled?
                (let [db-before @!db]
                  (swap! cursor-or-db Δ)
                  (let [db-after @!db
                        hydrated-mutation (hydrate-mutation db-before db-after mutation)]
                    (swap! !db update-tooling hydrated-mutation)))

                (swap! cursor-or-db Δ))

              (log/error "failed to process mutation:" id)))

          (log/debug "while time travelling, ignoring mutation:" id))))))


(defn make-emit-mutation

  "Returns a function that emits a mutation onto the mutation channel.

   mutations are vectors made up of the following:
   id - the id of the mutation
   cursor - if included, will operate on the cursor into the state, if not, will operate on the db
   Δ - a function that takes the db or the cursor and produces the desired change in state. "

  [<mutation]

  (fn [[id props]]
    (let [mutation [id (assoc props :emitted-at (t/now))]]
      (go (a/>! <mutation mutation)))))


(defn listen-for-emit-mutation

  "Listens for activity on the <mutation channel, upon which the received
   mutation will be either processed as a tooling mutation or processed normally."

  [{:keys [!db] :as Φ} <mutation process-mutation process-tooling-mutation]

  (go-loop []
    (let [[id _ :as mutation] (a/<! <mutation)
          tooling? (= (namespace id) "tooling")]
      (if tooling?
        (process-tooling-mutation mutation)
        (process-mutation mutation)))
    (recur)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; db setup


(defn make-db [config-opts]
  (r/atom {:playground {:heart 0
                        :star 3}
           :location {:path nil
                      :handler :unknown
                      :query nil}
           :comms {:chsk-status :init
                   :message {}
                   :post {}}
           :tooling {:tooling-active? true
                     :unprocessed-mutations '()
                     :processed-mutations '()
                     :state-browser-props {:cursored {:paths #{}
                                                      :upstream-paths #{}}
                                           :mutated {:paths #{}
                                                     :upstream-paths #{}}
                                           :collapsed #{[:tooling]}
                                           :focused {:paths #{}
                                                     :upstream-paths #{}}}
                     :cursors '()}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising state")
    (let [!db (make-db config-opts)
          <mutations (a/chan)
          emit-mutation! (make-emit-mutation <mutations)
          Φ {:config-opts config-opts
             :!db !db}
          process-mutation (make-process-mutation Φ)
          process-tooling-mutation (make-process-tooling-mutation Φ)]

      (log/info "begin listening for emitted mutations")
      (listen-for-emit-mutation Φ <mutations process-mutation process-tooling-mutation)

      (assoc component
             :!db !db
             :emit-mutation! emit-mutation!)))

  (stop [component] component))
