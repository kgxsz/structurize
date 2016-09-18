(ns structurize.styles.components.app-browser
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def app-browser
  [:.c-app-browser

   [:&__brace {:padding-top (-> v :nudge-xx-large px)}]

   [:&__node {:display :flex}

    [:&__value :&__key {:height (-> v :filling-small px)
                        :font-size (-> v :p-size-x-small px)
                        :padding-left (-> v :spacing-x-small px)
                        :padding-right (-> v :spacing-x-small px)
                        :border-radius (-> v :border-radius-small px)
                        :margin-bottom (-> v :spacing-xx-small px)
                        :background-color (:grey-c colours)
                        :white-space :nowrap}]

    [:&__key {:display :flex
              :align-items :center
              :margin-left (-> v :spacing-small px)
              :margin-right (-> v :spacing-xx-small px)
              :cursor :pointer}
     [:&--first {:margin-left 0}]
     [:&--written {:color (:light-green colours)}]
     [:&--downstream-focused {:background-color (:light-blue colours)
                              :color (:dark-blue colours)}]
     [:&--focused {:background-color (:dark-blue colours)
                   :color (:light-blue colours)}]]

    [:&__value {:display :flex
                :align-items :center}
     [:&--clickable {:cursor :pointer}]
     [:&--written {:color (:light-green colours)}]
     [:&--focused {:background-color (:light-blue colours)
                   :color (:dark-blue colours)}]]]])
