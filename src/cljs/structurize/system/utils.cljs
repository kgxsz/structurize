(ns structurize.system.utils
  (:require [clojure.data :as data]
            [cemerick.url :refer [map->query query->map]]
            [bidi.bidi :as b]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [taoensso.timbre :as log])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; general utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-upstream-paths
  "This function takes paths and returns a set of all sub-paths within them."
  [paths]

  (->> paths
       (map drop-last)
       (remove empty?)
       (map (partial reductions conj []))
       (map rest)
       (apply concat)
       set))


(defn make-all-paths
  "This function takes a map and returns a list of all paths in the map.
   For example {:a 1 :b {:c 2 :d 3}} would give ((:a) (:b :c) (:b :d))."
  [m]

  (if (or (not (map? m)) (empty? m))
    '(())
    (for [[k v] m
          subkey (make-all-paths v)]
      (cons k subkey))))


(defn make-paths
  "This function finds the path to every changed node between the two maps."
  [post pre]

  (let [[added removed _] (data/diff post pre)
        removed-paths (if removed (make-all-paths removed) [])
        added-paths (if added (make-all-paths added) [])]
    (into #{} (concat removed-paths added-paths))))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-side-effect (fn [_ id _] id))


(defmethod process-side-effect :default
  [_ id _]
  (log/warn "failed to process side-effect:" id))


(defn side-effect!

  "Dispatches a side-effect depending on the situation. Always allow tooling, browser and
   comms side effects. Otherwise ignore side effects when time travelling."

  ([Φ id] (side-effect! Φ id {}))
  ([{:keys [config-opts !state context] :as Φ} id props]
   (let [log? (get-in config-opts [:tooling :log?])
         time-travelling? (l/view-single @!state (in [:tooling :time-travelling?]))
         tooling? (:tooling? context)]

     (cond
       tooling? (do
                  (log-debug Φ "side-effect:" id)
                  (process-side-effect Φ id props))

       (not time-travelling?) (do
                                (log-debug Φ "side-effect:" id)
                                (process-side-effect Φ id props))

       :else (log-debug Φ "during time travel, ignoring side-effect:" id)))))
