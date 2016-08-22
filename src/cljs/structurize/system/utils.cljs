(ns structurize.system.utils)


(defn make-upstream-paths [paths]
  (->> paths
       (map drop-last)
       (remove empty?)
       (map (partial reductions conj []))
       (map rest)
       (apply concat)
       set))
