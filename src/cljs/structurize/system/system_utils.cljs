(ns structurize.system.system-utils)

(defn map-paths [m]
  (if (or (not (map? m))
          (empty? m))
    '(())
    (for [[k v] m
          subkey (map-paths v)]
      (cons k subkey))))


(defn upstream-paths [paths]
  (->> paths
       (map drop-last)
       (remove empty?)
       (map (partial reductions conj []))
       (map rest)
       (apply concat)
       set))
