(ns structurize.system.state
  (:require [structurize.system.system-utils :as u]
            [cljs-time.core :as t]
            [clojure.data :as d]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn build-diff [added removed]
  (let [paths (set (concat (u/map-paths added) (u/map-paths removed)))]
    (reduce
     (fn [a path] (assoc a path {:before (get-in removed path) :after (get-in added path)}))
     {}
     paths)))


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
                                        [added removed _] (d/diff post-Δ-db db)
                                        mutation-paths (into #{} (u/map-paths added))
                                        upstream-mutation-paths (u/upstream-paths mutation-paths)
                                        diff (build-diff added removed)
                                        updated-props (-> props
                                                          (dissoc :Δ)
                                                          (assoc :processed (t/now)
                                                                 :n (inc (:n (second previous-mutation) 0))
                                                                 :diff diff
                                                                 :mutation-paths mutation-paths
                                                                 :upstream-mutation-paths upstream-mutation-paths))
                                        updated-mutation [id updated-props]]

                                    (log/warn added)
                                    (log/warn removed)
                                    (log/warn mutation-paths)
                                    (log/warn diff)

                                    (-> post-Δ-db
                                        (update-in [:tooling :processed-mutations] (comp (partial take max-processed-mutations) (partial cons updated-mutation)))
                                        (assoc-in [:tooling :state-browser-props :mutated :paths] mutation-paths)
                                        (assoc-in [:tooling :state-browser-props :mutated :upstream-paths] upstream-mutation-paths)))))

          :else (log/debug "while time travelling, ignoring mutation:" id))))))


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
