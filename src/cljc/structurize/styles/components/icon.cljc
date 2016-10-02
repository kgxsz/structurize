(ns structurize.styles.components.icon
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def icon
  [:.c-icon {:text-decoration :none
             :color (-> vars :color :grey-a)}

   [:&--p-size
    (u/make-modifiers {:var :p-size :prop :font-size :unit px})]
   [:&--h-size
    (u/make-modifiers {:var :h-size :prop :font-size :unit px})]
   [:&--color
    (u/make-modifiers {:var :color})]
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
