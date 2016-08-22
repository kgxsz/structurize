(ns structurize.components.utils)


(defn ->class
  "Takes a set of keywords and transforms it
   into a string suitable for hiccup's class prop."
  [classes]
  (->> (map name classes)
       (interpose " ")
       (apply str)))


(defn without-propagation [& fs]
  "Takes functions to run, and stops the event propagation."
  (fn [e] (doseq [f fs] (f)) (.stopPropagation e)))




