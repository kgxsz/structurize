(ns structurize.styles.components.hero
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def hero
  [:.c-hero
   [:&__image {:min-height (-> vars :hero-image-min-height :medium px)
               :max-height (-> vars :hero-image-max-height :medium px)}]])

