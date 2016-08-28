(ns structurize.lens
  (:require [traversy.lens :as l]))

(defn in
  ([path] (in path nil))
  ([path not-found]
   (l/lens (fn [x]
              (list (get-in x path not-found)))
            (fn [f x]
              (update-in x path f)))))
