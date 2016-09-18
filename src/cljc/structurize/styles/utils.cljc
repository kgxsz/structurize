(ns structurize.styles.utils
  (:require [structurize.styles.vars :refer [vars]]
            [garden.color :as c]))

(defn make-modifiers [{:keys [var prop unit]}]
  (for [[k v] (get vars var)]
    [(keyword (str "&-" (name k))) {(or prop var) (if unit (unit v) v)}]))


(defn alpha [hex alpha]
  (assoc (c/hex->rgb hex) :alpha alpha))
