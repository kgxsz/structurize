(ns structurize.styles.components.text
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def text
  [:.c-text
   [:&--p-size
    (u/make-modifiers {:var :p-size :prop :font-size :unit px})]
   [:&--h-size
    (u/make-modifiers {:var :h-size :prop :font-size :unit px})]
   [:&--color
    (u/make-modifiers {:var :color})]])
