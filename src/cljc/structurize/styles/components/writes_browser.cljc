(ns structurize.styles.components.writes-browser
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def writes-browser
  [:.c-writes-browser
   [:&__controls {:background-color (u/alpha (-> vars :color :grey-a) (-> vars :alpha :low))
                  :border-radius (-> vars :border-radius :medium px)}

    [:&__item {:display :flex
               :justify-content :center
               :align-items :center
               :width (-> vars :filling :medium px)
               :height (-> vars :filling :medium px)
               :cursor :not-allowed
               :margin-bottom (-> vars :spacing :x-small px)
               :border-radius (-> vars :filling :medium (/ 2) px)
               :opacity (-> vars :alpha :low)}

     [:&:last-child {:margin-bottom 0}]
     [:&--green {:background-color (-> vars :color :light-green)}]
     [:&--yellow {:background-color (-> vars :color :light-yellow)}]
     [:&--opaque {:opacity 1}]
     [:&--clickable {:cursor :pointer}]]]

   [:&__item {:display :flex
              :flex-direction :column
              :padding (-> vars :spacing :medium px)
              :padding-right 0}
    [:&:last-child {:margin-right (-> vars :spacing :x-large px)}]

    [:&__superscript {:display :flex
                      :align-items :flex-end
                      :height (-> vars :filling :medium px)
                      :padding-left (-> vars :spacing :small px)
                      :padding-bottom (-> vars :spacing :xx-small px)}]

    [:&__pill {:padding (-> vars :spacing :x-small px)
               :border-style :dotted
               :border-width (-> vars :border-width :medium px)
               :border-radius (-> vars :border-radius :large px)}

     [:&__content {:display :flex
                   :align-items :center
                   :height (-> vars :filling :small px)
                   :padding-left (-> vars :spacing :x-small px)
                   :padding-right (-> vars :spacing :x-small px)
                   :border-radius (-> vars :border-radius :small px)
                   :background-color (-> vars :color :light-green)
                   :font-size (-> vars :p-size :x-small px)
                   :white-space :nowrap
                   :color (-> vars :color :dark-green)}]]]])
