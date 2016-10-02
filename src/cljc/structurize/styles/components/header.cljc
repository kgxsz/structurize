(ns structurize.styles.components.header
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))


(def header
  [:.c-header {:height (-> vars :header-height :medium px)
               :position :fixed
               :top 0
               :z-index 1
               :background-color (-> vars :color :white)}
   [:&__item {:background-color (-> vars :color :whitesmoke)}]])
