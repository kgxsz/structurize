(ns structurize.styles.components.header
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))


(def header
  [:.c-header {:height (-> v :header-height px)
               :position :fixed
               :padding-top (-> v :spacing-x-small px)
               :padding-bottom (-> v :spacing-x-small px)
               :top 0
               :z-index 1
               :background-color (:white-a colours)}
   [:&__item {:background-color (:white-b colours)}]])
