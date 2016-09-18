(ns structurize.styles.components.header
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))


(def header
  [:.c-header {:height (-> vars :header-height :medium px)
               :position :fixed
               :padding-top (-> vars :spacing :x-small px)
               :padding-bottom (-> vars :spacing :x-small px)
               :top 0
               :z-index 1
               :background-color (-> vars :color :white-a)}
   [:&__item {:background-color (-> vars :color :white-b)}]])
