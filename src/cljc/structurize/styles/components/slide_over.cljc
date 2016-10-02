(ns structurize.styles.components.slide-over
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def slide-over
  [:.c-slide-over {:height (percent 100)
                   :background-color (u/alpha (-> vars :color :white) (-> vars :alpha :none))
                   :position :absolute
                   :top 0
                   :transition-property :right
                   :transition-duration (-> vars :transition-duration :medium ms)}])
