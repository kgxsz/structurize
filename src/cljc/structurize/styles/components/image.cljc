(ns structurize.styles.components.image
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def image
  [:.c-image {:transition-property :opacity
              :transition-duration (-> vars :transition-duration :medium ms)}])
