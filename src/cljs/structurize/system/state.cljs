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


#_(defn make-emit-mutation* [{:keys [config-opts !db]}]
  (let [log? (get-in config-opts [:general :tooling :log?])
        tooling-disabled? (not (get-in config-opts [:tooling :enabled?]))
        max-processed-mutations (get-in config-opts [:tooling :max-processed-mutations])]

    (fn [[id {:keys [Δ] :as props} :as mutation]]

      (let [tooling-mutation? (= (namespace id) "tooling")]

        (when (or (not tooling-mutation?) log?)
          (log/debug "processing mutation:" id))

        (cond
          ;; tooling related mutations get swapped in no matter what
          tooling-mutation? (swap! !db Δ)

          ;; if tooling is disabled, swap in without any mutation decoration
          tooling-disabled? (swap! !db Δ)

          :else (swap! !db (fn [db]
                             (let [[_ previous-props] (first (get-in db [:tooling :processed-mutations]))
                                   pre-Δ-db (:post-Δ-db previous-props (dissoc db :tooling))
                                   post-Δ-db (Δ pre-Δ-db)
                                   mutation-paths (make-mutation-paths post-Δ-db pre-Δ-db)
                                   upstream-mutation-paths (u/upstream-paths mutation-paths)
                                   updated-props (-> props
                                                     (assoc :processed-at (t/now)
                                                            :n (inc (:n previous-props 0))
                                                            :pre-Δ-db pre-Δ-db
                                                            :post-Δ-db post-Δ-db
                                                            :pre-Δ-mutation-paths (:post-Δ-mutation-paths previous-props)
                                                            :post-Δ-mutation-paths mutation-paths
                                                            :pre-Δ-upstream-mutation-paths (:post-Δ-upstream-mutation-paths previous-props)
                                                            :post-Δ-upstream-mutation-paths upstream-mutation-paths))
                                   updated-mutation [id updated-props]]

                               (-> post-Δ-db
                                   (assoc :tooling (:tooling db))
                                   (update-in [:tooling :processed-mutations] (comp (partial take max-processed-mutations) (partial cons updated-mutation)))
                                   (assoc-in [:tooling :state-browser-props :mutated :paths] mutation-paths)
                                   (assoc-in [:tooling :state-browser-props :mutated :upstream-paths] upstream-mutation-paths))))))))))



(defn make-write-app! [config-opts !state]
  (fn [[id f]]
    (let [state @!state
          index (get-in state [:app-indices :read-write-index])
          pre-app (get-in state [:app-history index])
          post-app (f pre-app)
          paths (make-paths post-app pre-app)
          upstream-paths (u/make-upstream-paths paths)]
      (log/debug "write:" id)

      ;; TODO - When not live, act a little differently
      ;; when not live, don't incrememnt the track index
      ;; when not live, don't update the written props

      (swap! !state #(-> %
                         (update-in [:app-indices :read-write-index] inc)
                         (update-in [:app-indices :track-index] inc)
                         (assoc-in [:tooling :writes (inc index)] {:id id
                                                                   :n (inc index)
                                                                   :paths paths
                                                                   :upstream-paths upstream-paths
                                                                   :t (t/now)})
                         (assoc-in [:tooling :app-browser-props :written] {:paths paths
                                                                           :upstream-paths upstream-paths})
                         (assoc-in [:app-history (inc index)] post-app))))))


(defn make-write-tooling! [config-opts !state]
  (let [log? (get-in config-opts [:tooling :log?])]
    (fn [[id f]]
      (when log? (log/debug "write:" id))
      (swap! !state update-in [:tooling] f))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; db setup


(defn make-!state [config-opts]
  (r/atom {:app-indices {:track-index 0
                         :read-write-index 0}

           :app-history {0 {:playground {:heart 0
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

           :tooling {:tooling-active? true
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
                                            index (get-in state [:app-indices :track-index])]
                                        (f (v state (l/*> (l/in [:app-history index]) +lens)))))))

             :track-tooling (fn track
                              ([v +lens] (track v +lens identity))
                              ([v +lens f]
                               @(r/track #(f (v @!state (l/*> (l/in [:tooling]) +lens))))))

             :read-app (fn [v +lens]
                         (let [state @!state
                               index (get-in state [:app-indices :read-write-index])]
                           (v state (l/*> (l/in [:app-history index]) +lens))))

             :read-tooling (fn [v +lens]
                             (v @!state (l/*> (l/in [:tooling]) +lens)))

             :write-app! (make-write-app! config-opts !state)

             :write-tooling! (make-write-tooling! config-opts !state))))

  (stop [component] component))
