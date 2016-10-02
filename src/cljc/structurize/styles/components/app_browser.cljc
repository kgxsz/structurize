(ns structurize.styles.components.app-browser
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def app-browser
  [:.c-app-browser
   [:&__brace {:padding-top (-> vars :spacing :x-small px)}]

   [:&__node {:display :flex}

    [:&__value :&__key {:height (-> vars :filling :small px)
                        :font-size (-> vars :p-size :x-small px)
                        :padding-left (-> vars :spacing :x-small px)
                        :padding-right (-> vars :spacing :x-small px)
                        :border-radius (-> vars :border-radius :small px)
                        :margin-bottom (-> vars :spacing :xx-small px)
                        :background-color (u/alpha (-> vars :color :deepgrey) (-> vars :alpha :low))
                        :white-space :nowrap}]

    [:&__key {:display :flex
              :align-items :center
              :margin-left (-> vars :spacing :small px)
              :margin-right (-> vars :spacing :xx-small px)
              :cursor :pointer}
     [:&--first {:margin-left 0}]
     [:&--written {:color (-> vars :color :palegreen)}]
     [:&--downstream-focused {:background-color (-> vars :color :lightcyan)
                              :color (-> vars :color :dullblue)}]
     [:&--focused {:background-color (-> vars :color :dullblue)
                   :color (-> vars :color :lightcyan)}]]

    [:&__value {:display :flex
                :align-items :center}
     [:&--clickable {:cursor :pointer}]
     [:&--written {:color (-> vars :color :palegreen)}]
     [:&--focused {:background-color (-> vars :color :lightcyan)
                   :color (-> vars :color :dullblue)}]]]])
