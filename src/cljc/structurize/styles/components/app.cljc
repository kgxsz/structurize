(ns structurize.styles.components.app
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def app
  [:.c-app {:height (percent 100)
            :color (-> vars :color :grey-a)
            :font-family "'Raleway', Arial"
            :font-size (-> vars :p-size :medium px)}])
