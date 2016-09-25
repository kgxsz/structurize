(ns structurize.styles.components.tooling
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def tooling
  [:.c-tooling {:color (-> vars :color :white-b)
                :font-family "'Fira Mono', monospace"
                :font-size (-> vars :p-size :small px)}
   [:&__content {:width (percent 100)
                 :height (percent 100)
                 :padding (-> vars :spacing :medium px)
                 :background-color (u/alpha (-> vars :color :black-b) (-> vars :alpha :high))}]

   [:&__handle {:display :flex
                :justify-content :center
                :align-items :center
                :cursor :pointer
                :background-color (u/alpha (-> vars :color :black-b) (-> vars :alpha :high))
                :width (-> vars :filling :medium px)
                :height (-> vars :filling :medium px)
                :border-top-left-radius (-> vars :border-radius :medium px)
                :border-bottom-left-radius (-> vars :border-radius :medium px)
                :position :absolute
                :bottom (-> vars :spacing :medium px)
                :left (-> vars :filling :medium - px)}]


   [:&__item {:width (percent 100)
              :padding (-> vars :spacing :medium px)
              :background-color (u/alpha (-> vars :color :grey-a) (-> vars :alpha :low))
              :border-radius (-> vars :border-radius :medium px)
              :margin-bottom (-> vars :spacing :medium px)
              :overflow :auto}

    ["&::-webkit-scrollbar" {:display :none}]

    [:&:last-child {:margin-bottom 0}]]])
