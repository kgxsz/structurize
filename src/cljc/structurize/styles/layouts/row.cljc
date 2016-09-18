(ns structurize.styles.layouts.row
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def row
  [:.l-row {:display :flex
            :flex-direction :row}
   [:&--fill-parent {:width (percent 100)
                     :height (percent 100)}]
   [:&--height-100 {:height (percent 100)}]
   [:&--width-100 {:width (percent 100)}]
   [:&--justify
    [:&-center {:justify-content :center}]
    [:&-space-between {:justify-content :space-between}]
    [:&-start {:justify-content :flex-start}]
    [:&-end {:justify-content :flex-end}]]
   [:&__item
    [:&--grow {:flex-grow 1}]]])
