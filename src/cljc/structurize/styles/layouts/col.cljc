(ns structurize.styles.layouts.col
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def col
  [:.l-col {:display :flex
             :flex-direction :column}
    [:&--fill-parent {:width (percent 100)
                      :height (percent 100)}]
    [:&--height-100 {:height (percent 100)}]
    [:&--width-100 {:width (percent 100)}]
    [:&--justify
     [:&-center {:justify-content :center}]
     [:&-space-between {:justify-content :space-between}]
     [:&-start {:justify-content :flex-start}]
     [:&-end {:justify-content :flex-end}]]
    [:&--align
     [:&-center {:align-items :center}]
     [:&-start {:align-items :flex-start}]
     [:&-end {:align-items :flex-end}]]
    [:&--margin
     [:&-top
      [:&-small {:margin-top (-> v :spacing-small px)}]
      [:&-medium {:margin-top (-> v :spacing-medium px)}]
      [:&-xxx-large {:margin-top (-> v :spacing-xxx-large px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]
      [:&-medium {:margin-bottom (-> v :spacing-medium px)}]]
     [:&-right
      [:&-small {:margin-right (-> v :spacing-small px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]]
     [:&-left
      [:&-small {:margin-left (-> v :spacing-small px)}]]]
    [:&__item
     [:&--grow {:flex-grow 1}]]])
