(ns structurize.system.state
  (:require [structurize.system.system-utils :as u]
            [cljs-time.core :as t]
            [clojure.data :as data]
            [com.stuartsierra.component :as component]
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


(defn make-mutation-paths

  "This function finds the path to every mutation in the db."

  [post-Δ-db pre-Δ-db]

  (let [[added removed _] (data/diff post-Δ-db pre-Δ-db)
        removed-paths (if removed (map->paths removed) [])
        added-paths (if added (map->paths added) [])]
     (into #{} (concat removed-paths added-paths))))


(defn make-emit-mutation [{:keys [config-opts !db]}]
  (let [log? (get-in config-opts [:general :tooling :log?])
        tooling-disabled? (not (get-in config-opts [:tooling :enabled?]))
        max-processed-mutations (get-in config-opts [:tooling :max-processed-mutations])]

    (fn [[id {:keys [Δ] :as props} :as mutation]]

      (let [tooling-mutation? (= (namespace id) "tooling")
            real-time? (empty? (get-in @!db [:tooling :unprocessed-mutations]))]

        (when (or (not tooling-mutation?) log?)
          (log/debug "processing mutation:" id))

        (cond
          ;; tooling related mutations get swapped in no matter what
          tooling-mutation? (swap! !db Δ)

          ;; if tooling is disabled, swap in without any mutation decoration
          tooling-disabled? (swap! !db Δ)

          :else (swap! !db (fn [db]
                             (let [[_ previous-props] (if real-time?
                                                        (first (get-in db [:tooling :processed-mutations]))
                                                        (last (get-in db [:tooling :unprocessed-mutations])))
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

                               (if real-time?

                                 (-> post-Δ-db
                                     (assoc :tooling (:tooling db))
                                     (update-in [:tooling :processed-mutations] (comp (partial take max-processed-mutations) (partial cons updated-mutation)))
                                     (assoc-in [:tooling :state-browser-props :mutated :paths] mutation-paths)
                                     (assoc-in [:tooling :state-browser-props :mutated :upstream-paths] upstream-mutation-paths))

                                 (update-in db [:tooling :unprocessed-mutations] concat [updated-mutation]))))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; db setup


(defn make-db [config-opts]
  (r/atom {:playground {:heart 0
                        :star 3
                        :ping 0
                        :pong 0}
           :location {:path nil
                      :handler :unknown
                      :query nil}
           :comms {:chsk-status :init
                   :message {}
                   :post {}}
           :tooling {:tooling-active? true
                     :unprocessed-mutations '()
                     :processed-mutations '()
                     :state-browser-props {:mutated {:paths #{}
                                                     :upstream-paths #{}}
                                           :collapsed #{[:tooling]
                                                        [:tooling :unprocessed-mutations]
                                                        [:tooling :processed-mutations]}
                                           :focused {:paths #{}
                                                     :upstream-paths #{}}}}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising state")
    (let [!db (make-db config-opts)]
      (assoc component
             :!db !db
             :emit-mutation! (make-emit-mutation {:config-opts config-opts :!db !db}))))

  (stop [component] component))
