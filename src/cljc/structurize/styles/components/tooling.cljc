(ns structurize.styles.components.tooling
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def tooling
  [:.c-tooling {:color (:white-b colours)
                :font-family "'Fira Mono', monospace"
                :font-size (-> v :p-size-small px)
                :width (percent 100)
                :height (percent 100)
                :padding (-> v :spacing-medium px)
                :background-color (u/alpha (:black-b colours) (:alpha-high v))}

   [:&__handle {:display :flex
                :justify-content :center
                :align-items :center
                :cursor :pointer
                :background-color (u/alpha (:black-b colours) (:alpha-high v))
                :width (-> v :filling-medium px)
                :height (-> v :filling-medium px)
                :border-top-left-radius (-> v :border-radius-medium px)
                :border-bottom-left-radius (-> v :border-radius-medium px)
                :position :absolute
                :bottom (-> v :spacing-medium px)
                :left (-> v :filling-medium - px)}]


   [:&__item {:width (percent 100)
              :padding (-> v :spacing-medium px)
              :background-color (u/alpha (:grey-a colours) (:alpha-low v))
              :border-radius (-> v :border-radius-medium px)
              :margin-bottom (-> v :spacing-medium px)
              :overflow :auto}

    ["&::-webkit-scrollbar" {:display :none}]

    [:&:last-child {:margin-bottom 0}]]])
