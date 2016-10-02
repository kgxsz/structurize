(ns structurize.styles.components.masthead
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def masthead
  [:.c-masthead {:height (-> vars :masthead-height :medium px)
                 :position :relative
                 :background-color (-> vars :color :white)}
   [:&__lip {:height (-> vars :masthead-height :medium px)
             :position :absolute
             :left 0
             :right 0
             :bottom (-> vars :masthead-height :medium px)
             :background-color (u/alpha (-> vars :color :deepgrey) (-> vars :alpha :medium))}]
   [:&__avatar {:width (-> vars :avatar-width :medium px)
                :height (-> vars :avatar-height :medium px)
                :position :relative
                :bottom (px (- (-> vars :avatar-height :medium) (-> vars :spacing :xx-large)))
                :z-index 0;
                :background-color (u/alpha (-> vars :color :deepgrey) (-> vars :alpha :low))
                :overflow :hidden
                :border-style :solid
                :border-color (-> vars :color :white)
                :border-width (-> vars :border-width :large px)
                :border-radius (-> vars :avatar-height :medium px)}]])

