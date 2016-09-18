(ns structurize.styles.components.icon
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def icon
  [:.c-icon
   [:&--h-size-xx-large {:font-size (-> vars :h-size :xx-large px)}]
   [:&--h-size-large {:font-size (-> vars :h-size :large px)}]])
