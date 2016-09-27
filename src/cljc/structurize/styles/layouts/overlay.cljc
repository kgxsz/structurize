(ns structurize.styles.layouts.overlay
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def overlay
  [:.l-overlay {:width (percent 100)
                :height (percent 100)
                :pointer-events :none
                :position :absolute
                :background-color (-> vars :color :tranparent)
                :z-index 1
                :top 0
                :left 0}
   [:&--fill-viewport {:width (percent 100)
                       :height (percent 100)
                       :position :fixed}]])
