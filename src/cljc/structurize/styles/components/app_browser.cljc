(ns structurize.styles.components.app-browser
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def app-browser
  [:.c-app-browser

   [:&__brace {:padding-top (-> vars :nudge :xx-large px)}]

   [:&__node {:display :flex}

    [:&__value :&__key {:height (-> vars :filling :small px)
                        :font-size (-> vars :p-size :x-small px)
                        :padding-left (-> vars :spacing :x-small px)
                        :padding-right (-> vars :spacing :x-small px)
                        :border-radius (-> vars :border-radius :small px)
                        :margin-bottom (-> vars :spacing :xx-small px)
                        :background-color (-> vars :color :grey-c)
                        :white-space :nowrap}]

    [:&__key {:display :flex
              :align-items :center
              :margin-left (-> vars :spacing :small px)
              :margin-right (-> vars :spacing :xx-small px)
              :cursor :pointer}
     [:&--first {:margin-left 0}]
     [:&--written {:color (-> vars :color :light-green)}]
     [:&--downstream-focused {:background-color (-> vars :color :light-blue)
                              :color (-> vars :color :dark-blue)}]
     [:&--focused {:background-color (-> vars :color :dark-blue)
                   :color (-> vars :color :light-blue)}]]

    [:&__value {:display :flex
                :align-items :center}
     [:&--clickable {:cursor :pointer}]
     [:&--written {:color (-> vars :color :light-green)}]
     [:&--focused {:background-color (-> vars :color :light-blue)
                   :color (-> vars :color :dark-blue)}]]]])
