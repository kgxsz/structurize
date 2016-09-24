(ns structurize.styles.layouts.cell
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def cell
  [:.l-cell
   [:&--fill-parent {:width (percent 100)
                     :height (percent 100)}]

   [:&--height-100 {:height (percent 100)}]
   [:&--height
    (u/make-modifiers {:var :filling :prop :height :unit px})]

   [:&--width-100 {:width (percent 100)}]
   [:&--width
    (u/make-modifiers {:var :filling :prop :width :unit px})]

   [:&--justify
    [:&-center {:display :flex
                :flex-direction :row
                :justify-content :center}]
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
    [:&-start {:display :flex
               :flex-direction :row
               :align-items :flex-start}]
    [:&-end {:display :flex
             :flex-direction :row
             :align-items :flex-end}]]

   [:&--margin-top
    (u/make-modifiers {:var :proportion :prop :margin-top :unit vh})
    (u/make-modifiers {:var :spacing :prop :margin-top :unit px})]
   [:&--margin-right
    (u/make-modifiers {:var :proportion :prop :margin-right :unit vw})
    (u/make-modifiers {:var :spacing :prop :margin-right :unit px})]
   [:&--margin-bottom
    (u/make-modifiers {:var :proportion :prop :margin-bottom :unit vh})
    (u/make-modifiers {:var :spacing :prop :margin-bottom :unit px})]
   [:&--margin-left
    (u/make-modifiers {:var :proportion :prop :margin-left :unit vw})
    (u/make-modifiers {:var :spacing :prop :margin-left :unit px})]

   [:&--padding-top
    (u/make-modifiers {:var :proportion :prop :padding-top :unit vh})
    (u/make-modifiers {:var :spacing :prop :padding-top :unit px})]
   [:&--padding-right
    (u/make-modifiers {:var :proportion :prop :padding-right :unit vw})
    (u/make-modifiers {:var :spacing :prop :padding-right :unit px})]
   [:&--padding-bottom
    (u/make-modifiers {:var :proportion :prop :padding-bottom :unit vh})
    (u/make-modifiers {:var :spacing :prop :padding-bottom :unit px})]
   [:&--padding-left
    (u/make-modifiers {:var :proportion :prop :padding-left :unit vw})
    (u/make-modifiers {:var :spacing :prop :padding-left :unit px})]])
