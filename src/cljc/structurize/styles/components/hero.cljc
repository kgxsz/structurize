(ns structurize.styles.components.hero
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def hero
  [:.c-hero
   [:&__image {:min-height (-> v :hero-image-min-height px)
               :max-height (-> v :hero-image-max-height px)}]])

