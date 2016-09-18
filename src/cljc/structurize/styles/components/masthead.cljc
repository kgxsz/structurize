(ns structurize.styles.components.masthead
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def masthead
  [:.c-masthead {:height (-> vars :masthead-height :medium px)
                 :position :relative
                 :background-color (-> vars :color :white-a)}
   [:&__lip {:height (-> vars :masthead-height :medium px)
             :position :absolute
             :top (-> vars :masthead-height :medium - px)
             :background-color (u/alpha (-> vars :color :black-b) (-> vars :alpha :medium))}]
   [:&__avatar {:width (-> vars :avatar-width :medium px)
                :height (-> vars :avatar-height :medium px)
                :position :absolute
                :bottom (-> vars :spacing :medium px)
                :z-index 0;
                :background-color (u/alpha (-> vars :color :black-b) (-> vars :alpha :low))
                :overflow :hidden
                :border-style :solid
                :border-color (-> vars :color :white-a)
                :border-width (-> vars :border-width :large px)
                :border-radius (-> vars :avatar-height :medium px)}]])

