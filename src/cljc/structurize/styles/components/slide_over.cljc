(ns structurize.styles.components.slide-over
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def slide-over
  [:.c-slide-over {:height (percent 100)
                   :background-color (:transparent colours)
                   :position :absolute
                   :top 0
                   :transition-property :right
                   :transition-duration (-> v :transition-duration ms)}])
