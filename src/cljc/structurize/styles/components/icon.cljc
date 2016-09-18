(ns structurize.styles.components.icon
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def icon
  [:.c-icon
   [:&--h-size-xx-large {:font-size (-> v :h-size-xx-large px)}]
   [:&--h-size-large {:font-size (-> v :h-size-large px)}]])
