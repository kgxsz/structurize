(ns structurize.styles.layouts.col
  (:require [structurize.styles.vars :refer [vars]]
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
     [:&-space-around {:justify-content :space-around}]
     [:&-start {:justify-content :flex-start}]
     [:&-end {:justify-content :flex-end}]]

    [:&--align
     [:&-center {:align-items :center}]
     [:&-start {:align-items :flex-start}]
     [:&-end {:align-items :flex-end}]]

   [:&__item
    [:&--grow {:flex-grow 1}]]

   [:&--margin-top
    (u/make-modifiers {:var :spacing :prop :margin-top :unit px})]
   [:&--margin-right
    (u/make-modifiers {:var :spacing :prop :margin-right :unit px})]
   [:&--margin-bottom
    (u/make-modifiers {:var :spacing :prop :margin-bottom :unit px})]
   [:&--margin-left
    (u/make-modifiers {:var :spacing :prop :margin-left :unit px})]

   [:&--padding-top
    (u/make-modifiers {:var :spacing :prop :padding-top :unit px})]
   [:&--padding-right
    (u/make-modifiers {:var :spacing :prop :padding-right :unit px})]
   [:&--padding-bottom
    (u/make-modifiers {:var :spacing :prop :padding-bottom :unit px})]
   [:&--padding-left
    (u/make-modifiers {:var :spacing :prop :padding-left :unit px})]])
