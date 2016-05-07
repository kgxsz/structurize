(ns structurize.system.state
  (:require [structurize.system.system-utils :as u]
            [cljs-time.core :as t]
            [clojure.data :as data]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

;; TODO - diffing is feeling wierd because of paths being nilled instead of being dissoc'd, maybe I should be saving the entire state?
;; If I were to put the entire state tree into the first guy, and then assoc the changes onto the next guy, so on and so fourth, then
;; I'd be sharing the same data structure right? But how do I get my hands on that diff? and how do I ignore the whole tooling side of things?
;; I could apply the changes to the entire db within a swap, and  surreptitiously shove it directly into the mutation... it'll be shared memory!


(defn map->paths

  ;; TODO - make map paths deal with nils, and output a set of the paths

  "This function takes a map and returns a list of every possible path in the map.
   For example {:a 1 :b {:c 2 :d 3}} would give ((:a) (:b :c) (:b :d))."

  [m]

  (if (or (not (map? m)) (empty? m))
    '(())
    (for [[k v] m
          subkey (map->paths v)]
      (cons k subkey))))


(defn make-mutation-paths

  ;; TODO - make sure that you properly light up the excluded paths that show a map change
  "This function finds every path  "

  [added removed]

  (let [removed-paths (if removed (map->paths removed) [])
        added-paths (if added (map->paths added) [])
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


(defn make-diff [added removed mutation-paths]
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


          ;; Start with the initial DB, and to get the next one by applying the mutation over the last db then swap it in.


          real-time? (swap! !db (fn [db]
                                  (let [[_ previous-props] (first (get-in db [:tooling :processed-mutations]))
                                        pre-Δ-db (:post-Δ-db previous-props (dissoc db :tooling))
                                        post-Δ-db (Δ pre-Δ-db)
                                        [added removed _] (data/diff post-Δ-db pre-Δ-db)

                                        mutation-paths (make-mutation-paths added removed)
                                        upstream-mutation-paths (u/upstream-paths mutation-paths)
                                        #_diff #_(make-diff added removed mutation-paths)
                                        updated-props (-> props
                                                          (assoc :processed (t/now)
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
             :emit-mutation! (make-emit-mutation* {:config-opts config-opts :!db !db}))))

  (stop [component] component))
