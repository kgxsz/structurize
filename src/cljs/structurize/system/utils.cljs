(ns structurize.system.utils
  (:require [clojure.data :as data]
            [cemerick.url :refer [map->query query->map]]))

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
