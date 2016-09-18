(ns structurize.styles.components.text
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def text
  [:.c-text
   [:&--p-size
    [:&-small {:font-size (-> v :p-size-small px)}]
    [:&-large {:font-size (-> v :p-size-large px)}]
    [:&-xx-large {:font-size (-> v :p-size-xx-large px)}]]
   [:&--h-size
    [:&-small {:font-size (-> v :h-size-small px)}]
    [:&-large {:font-size (-> v :h-size-large px)}]
    [:&-xx-large {:font-size (-> v :h-size-xx-large px)}]]
   [:&--color-white-a {:color (:white-a colours)}]])
