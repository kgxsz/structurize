(ns structurize.styles.layouts.cell
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def cell
  [:.l-cell
    [:&--fill-parent {:width (percent 100)
                      :height (percent 100)}]
    [:&--height-100 {:height (percent 100)}]
    [:&--width-100 {:width (percent 100)}]
    [:&--justify
     [:&-center {:display :flex
                 :flex-direction :row
                 :justify-content :center}]
     [:&-space-between {:display :flex
                        :flex-direction :row
                        :justify-content :space-between}]
     [:&-start {:display :flex
                :flex-direction :row
                :justify-content :flex-start}]
     [:&-end {:display :flex
              :flex-direction :row
              :justify-content :flex-end}]]
    [:&--align
     [:&-center {:display :flex
                 :flex-direction :row
                 :align-items :center}]
     [:&-end {:display :flex
              :flex-direction :row
              :align-items :flex-end}]]
    [:&--margin
     [:&-top
      [:&-small {:margin-top (-> v :spacing-small px)}]
      [:&-medium {:margin-top (-> v :spacing-medium px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]
      [:&-medium {:margin-bottom (-> v :spacing-medium px)}]]
     [:&-right
      [:&-small {:margin-right (-> v :spacing-small px)}]
      [:&-medium {:margin-right (-> v :spacing-medium px)}]
      [:&-large {:margin-right (-> v :spacing-large px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]]
     [:&-left
      [:&-small {:margin-left (-> v :spacing-small px)}]
      [:&-medium {:margin-left (-> v :spacing-medium px)}]
      [:&-large {:margin-left (-> v :spacing-large px)}]]]])

