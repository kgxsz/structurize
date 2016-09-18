(ns structurize.styles.utils
  (:require [garden.color :as c]))

(defn alpha [hex alpha]
  (assoc (c/hex->rgb hex) :alpha alpha))
