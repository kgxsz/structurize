(ns structurize.styles.components.writes-browser
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def writes-browser
  [:.c-writes-browser
    [:&__controls {:background-color (u/alpha (:grey-a colours) (:alpha-low v))
                   :border-radius (-> v :border-radius-medium px)
                   :padding (-> v :spacing-medium px)}

     [:&__item {:display :flex
                :justify-content :center
                :align-items :center
                :width (-> v :filling-medium px)
                :height (-> v :filling-medium px)
                :cursor :not-allowed
                :margin-bottom (-> v :spacing-x-small px)
                :border-radius (-> v :filling-medium (/ 2) px)
                :font-size (-> v :p-size-small px)
                :opacity (:alpha-low v)}

      [:&:last-child {:margin-bottom 0}]

      [:&--green {:background-color (:light-green colours)
                  :color (:dark-green colours)}]

      [:&--yellow {:background-color (:light-yellow colours)
                   :color (:dark-yellow colours)}]

      [:&--opaque {:opacity 1}]

      [:&--clickable {:cursor :pointer}]]]

    [:&__item {:padding (-> v :spacing-medium px)
               :padding-right 0}

     [:&:last-child {:margin-right (-> v :spacing-x-large px)}]]

    [:&__pill-superscript {:display :flex
                           :align-items :flex-end
                           :height (-> v :filling-medium px)
                           :padding-left (-> v :spacing-small px)
                           :padding-bottom (-> v :spacing-xx-small px)
                           :font-size (-> v :p-size-small px)}
     [:&__symbol {:margin-right (-> v :spacing-xx-small px)}]]

    [:&__pill {:padding (-> v :spacing-x-small px)
               :border-style :dotted
               :border-width (-> v :border-width-medium px)
               :border-radius (-> v :border-radius-large px)}

     [:&__content {:display :flex
                   :align-items :center
                   :height (-> v :filling-small px)
                   :padding-left (-> v :spacing-x-small px)
                   :padding-right (-> v :spacing-x-small px)
                   :border-radius (-> v :border-radius-small px)
                   :background-color (:light-green colours)
                   :font-size (-> v :p-size-x-small px)
                   :white-space :nowrap
                   :color (:dark-green colours)}]]])
