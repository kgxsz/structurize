(ns structurize.components.utils)


(defn ->class
  "Takes a map of keywords and booleans and transforms it
   into a class string suitable for hiccup's class prop."
  [classes]
  (->> classes
       (filter second)
       (map (comp name first))
       (interpose " ")
       (apply str)))


(defn without-propagation [& fs]
  "Takes functions to run, and stops the event propagation."
  (fn [e] (doseq [f fs] (f)) (.stopPropagation e)))
