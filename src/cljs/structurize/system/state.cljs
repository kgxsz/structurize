(ns structurize.system.state
  (:require [structurize.system.system-utils :as u]
            [cljs-time.core :as t]
            [clojure.data :as data]
            [com.stuartsierra.component :as component]
            [traversy.lens :as l]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn map->paths
  "This function takes a map and returns a list of every possible path in the map.
   For example {:a 1 :b {:c 2 :d 3}} would give ((:a) (:b :c) (:b :d))."
  [m]

  (if (or (not (map? m)) (empty? m))
    '(())
    (for [[k v] m
          subkey (map->paths v)]
      (cons k subkey))))


(defn make-paths
  "This function finds the path to every changed node between the two maps."
  [post pre]

  (let [[added removed _] (data/diff post pre)
        removed-paths (if removed (map->paths removed) [])
        added-paths (if added (map->paths added) [])]
     (into #{} (concat removed-paths added-paths))))


(defn make-write-app! [config-opts !state]
  (fn [[id f]]
    (let [state @!state
          index (get-in state [:tooling :read-write-index])
          real-time? (= (get-in state [:tooling :read-write-index])
                        (get-in state [:tooling :track-index]))
          pre-app (get-in state [:app-history index])
          post-app (f pre-app)
          paths (make-paths post-app pre-app)
          upstream-paths (u/make-upstream-paths paths)]

      (log/debug "write:" id)

      (swap! !state #(cond-> %
                       true (update-in [:tooling :read-write-index] inc)
                       real-time? (update-in [:tooling :track-index] inc)
                       true (assoc-in [:tooling :writes (inc index)] {:id id
                                                                   :n (inc index)
                                                                   :paths paths
                                                                   :upstream-paths upstream-paths
                                                                   :t (t/now)})
                       real-time? (assoc-in [:tooling :app-browser-props :written] {:paths paths
                                                                           :upstream-paths upstream-paths})
                       true (assoc-in [:app-history (inc index)] post-app))))))


(defn make-write-tooling! [config-opts !state]
  (let [log? (get-in config-opts [:tooling :log?])]
    (fn [[id f]]
      (when log? (log/debug "write:" id))
      (swap! !state update :tooling f))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; db setup


(defn make-!state [config-opts]
  (r/atom {:app-history {0 {:playground {:heart 0
                                         :star 3
                                         :ping 0
                                         :pong 0}
                            :location {:path nil
                                       :handler :unknown
                                       :query nil}
                            :app-status :uninitialised
                            :comms {:chsk-status :initialising
                                    :message {}
                                    :post {}}
                            :auth {}}}

           :tooling {:track-index 0
                     :read-write-index 0
                     :tooling-active? true
                     :writes {}
                     :app-browser-props {:written {:paths #{}
                                                   :upstream-paths #{}}
                                         :collapsed #{}
                                         :focused {:paths #{}
                                                   :upstream-paths #{}}}}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord State [config-opts]
  component/Lifecycle
  (start [component]
    (log/info "initialising state")
    (let [!state (make-!state config-opts)]
      (assoc component
             :track-app (fn track
                          ([v +lens] (track v +lens identity))
                          ([v +lens f]
                           @(r/track #(let [state @!state
                                            index (get-in state [:tooling :track-index])]
                                        (f (v state (l/*> (l/in [:app-history index]) +lens)))))))

             :track-tooling (fn track
                              ([v +lens] (track v +lens identity))
                              ([v +lens f]
                               @(r/track #(f (v @!state (l/*> (l/in [:tooling]) +lens))))))

             :read-app (fn [v +lens]
                         (let [state @!state
                               index (get-in state [:tooling :read-write-index])]
                           (v state (l/*> (l/in [:app-history index]) +lens))))

             :read-tooling (fn [v +lens]
                             (v @!state (l/*> (l/in [:tooling]) +lens)))

             :write-app! (make-write-app! config-opts !state)

             :write-tooling! (make-write-tooling! config-opts !state))))

  (stop [component] component))
