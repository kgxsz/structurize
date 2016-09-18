(ns structurize.styles.components.page
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def page
  [:.c-page {:padding-top (-> vars :header-height :medium px)
             :padding-bottom (-> vars :spacing :xx-large px)}])

