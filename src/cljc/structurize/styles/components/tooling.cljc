(ns structurize.styles.components.tooling
  (:require [structurize.styles.vars :refer [vars]]
            [structurize.styles.utils :as u]
            [garden.units :refer [px percent ms vh vw]]))

(def tooling
  [:.c-tooling {:color (-> vars :color :white-b)
                :font-family "'Fira Mono', monospace"
                :font-size (-> vars :p-size :small px)}

   [:&__content {:pointer-events :auto
                 :background-color (u/alpha (-> vars :color :black-b) (-> vars :alpha :high))}]

   [:&__handle {:cursor :pointer
                :background-color (u/alpha (-> vars :color :black-b) (-> vars :alpha :high))
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
