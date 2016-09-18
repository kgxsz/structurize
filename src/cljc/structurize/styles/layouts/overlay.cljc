(ns structurize.styles.layouts.overlay
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def overlay
  [:.l-overlay {:width (percent 100)
                :height (percent 100)
                :pointer-events :none
                :position :absolute
                :background-color (:tranparent colours)
                :z-index 1
                :top 0
                :left 0}
   [:&--fill-viewport {:width (vw 100)
                       :height (vh 100)
                       :position :fixed}]
   [:&__content {:pointer-events :auto}]])
