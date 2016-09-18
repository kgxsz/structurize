(ns structurize.styles.components.page
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def page
  [:.c-page {:padding-top (-> v :header-height px)
             :padding-bottom (-> v :spacing-xx-large px)}])

