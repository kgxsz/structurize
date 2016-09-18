(ns structurize.styles.components.image
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def image
  [:.c-image {:transition-property :opacity
              :transition-duration (-> v :transition-duration ms)}
   [:&--transparent {:opacity 0}]])
