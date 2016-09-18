(ns structurize.styles.components.masthead
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def masthead
  [:.c-masthead {:height (-> v :masthead-height px)
                 :position :relative
                 :background-color (:white-a colours)}
   [:&__lip {:height (-> v :masthead-height px)
             :position :absolute
             :top (-> v :masthead-height - px)
             :background-color (u/alpha (:black-b colours) (:alpha-medium v))}]
   [:&__avatar {:width (-> v :avatar-width-medium px)
                :height (-> v :avatar-height-medium px)
                :position :absolute
                :bottom (-> v :spacing-medium px)
                :z-index 0;
                :background-color (u/alpha (:black-b colours) (:alpha-low v))
                :overflow :hidden
                :border-style :solid
                :border-color (:white-a colours)
                :border-width (-> v :border-width-large px)
                :border-radius (-> v :avatar-height-medium px)}]])

