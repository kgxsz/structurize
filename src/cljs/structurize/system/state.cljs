(ns structurize.system.state
  (:require [structurize.system.system-utils :as u]
            [cljs-time.core :as t]
            [clojure.data :as data]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn map-paths [m]
  (if (or (not (map? m)) (empty? m))
    '(())
    (for [[k v] m
          subkey (map-paths v)]
      (cons k subkey))))


(defn build-mutation-paths [added removed]
  (let [removed-paths (if removed (map-paths removed) [])
        added-paths (if added (map-paths added) [])
        mutation-paths (into #{} (concat removed-paths added-paths))
        excluded-mutation-paths (into #{} (for [primary-path mutation-paths
                                                secondary-path mutation-paths
                                                :when (and (not= primary-path secondary-path)
                                                           (> (count primary-path) (count secondary-path))
                                                           (->> (map vector primary-path secondary-path)
                                                                (remove (fn [[a b]] (= a b)))
                                                                empty?))]
                                            primary-path))]
    (set/difference mutation-paths excluded-mutation-paths)))


(defn build-diff [added removed mutation-paths]
  (reduce
   (fn [a path] (assoc a path {:before (get-in removed path) :after (get-in added path)}))
   {}
   mutation-paths))


(defn make-emit-mutation* [{:keys [config-opts !db]}]
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

          ;; only swap if we're in real time, apply the mutation Δ and any tooling related information
          real-time? (swap! !db (fn [db]
                                  (let [post-Δ-db (Δ db)
                                        previous-mutation (first (get-in db [:tooling :processed-mutations]))
                                        [added removed _] (data/diff post-Δ-db db)
                                        mutation-paths (build-mutation-paths added removed)
                                        upstream-mutation-paths (u/upstream-paths mutation-paths)
                                        diff (build-diff added removed mutation-paths)
                                        updated-props (-> props
                                                          (dissoc :Δ)
                                                          (assoc :processed (t/now)
                                                                 :n (inc (:n (second previous-mutation) 0))
                                                                 :diff diff
                                                                 :mutation-paths mutation-paths
                                                                 :upstream-mutation-paths upstream-mutation-paths))
                                        updated-mutation [id updated-props]]

                                    (-> post-Δ-db
                                        (update-in [:tooling :processed-mutations] (comp (partial take max-processed-mutations) (partial cons updated-mutation)))
                                        (assoc-in [:tooling :state-browser-props :mutated :paths] mutation-paths)
                                        (assoc-in [:tooling :state-browser-props :mutated :upstream-paths] upstream-mutation-paths)))))

          :else (log/debug "while time travelling, ignoring mutation:" id))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; db setup


(defn make-db [config-opts]
  (r/atom {:playground {:heart 0
                        :star 3
                        :edit :false}
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
                                           :collapsed #{[:tooling]}
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
             :emit-mutation! (make-emit-mutation* {:config-opts config-opts :!db !db}))))

  (stop [component] component))
