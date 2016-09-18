(ns structurize.styles.variables
  (:require [garden.color :as c]))

(def colours
  {:tranparent (c/rgba [0 0 0 0])
   :black-a "#000000"
   :black-b "#000A14"
   :white-a "#FFFFFF"
   :white-b "#F2F2F2"
   :white-c "#DDDDDD"
   :grey-a "#505A64"
   :grey-b "#343337"
   :grey-c "#474C51"
   :light-green "#D3EDA3"
   :green "#5EB95E"
   :dark-green "#73962E"
   :light-purple "#DDAEFF"
   :purple "#8058A5"
   :dark-purple "#8156A7"
   :light-yellow "#FCEBBD"
   :yellow "#FAD232"
   :dark-yellow "#AF9540"
   :light-red "#F5AB9E"
   :red "#DD514C"
   :dark-red "#8C3A2B"
   :light-orange "#E77400"
   :orange "#F37B1D"
   :dark-orange "#FEC58D"
   :light-blue "#E1F2FA"
   :blue "#1F8DD6"
   :dark-blue "#5992AA"})

(def v
  {:p-size-xx-small 8
   :p-size-x-small 10
   :p-size-small 12
   :p-size-medium 14
   :p-size-large 16
   :p-size-x-large 18
   :p-size-xx-large 20

   :h-size-xx-small 25
   :h-size-x-small 30
   :h-size-small 35
   :h-size-medium 40
   :h-size-large 60
   :h-size-x-large 80
   :h-size-xx-large 100

   :spacing-xx-small 2
   :spacing-x-small 6
   :spacing-small 8
   :spacing-medium 10
   :spacing-large 15
   :spacing-x-large 20
   :spacing-xx-large 30
   :spacing-xxx-large 50

   :nudge-small 1
   :nudge-medium 2
   :nudge-large 3
   :nudge-x-large 4
   :nudge-xx-large 6

   :filling-small 22
   :filling-medium 26
   :filling-large 32

   :border-width-small 1
   :border-width-medium 2
   :border-width-large 3

   :border-radius-x-small 2
   :border-radius-small 3
   :border-radius-medium 4
   :border-radius-large 5
   :border-radius-x-large 6
   :border-radius-xx-large 7

   :button-width-medium 220

   :button-height-medium 50

   :avatar-width-medium 130

   :avatar-height-medium 130

   :header-height 50
   :masthead-height 35

   :hero-image-min-height 120
   :hero-image-max-height 480

   :transition-duration 400

   :proportion-x-small 25
   :proportion-small 30
   :proportion-medium 50
   :proportion-large 70

   :alpha-low 0.3
   :alpha-medium 0.5
   :alpha-high 0.7})

