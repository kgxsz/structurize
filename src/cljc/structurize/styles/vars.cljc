(ns structurize.styles.vars
  (:require [garden.color :as c]))

(def vars
  {:color {:tranparent (c/rgba [0 0 0 0])
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
           :dark-blue "#5992AA"}

   :p-size {:xx-small 8
            :x-small 10
            :small 12
            :medium 14
            :large 16
            :x-large 18
            :xx-large 20}

   :h-size {:h-size-xx-small 25
            :h-size-x-small 30
            :h-size-small 35
            :h-size-medium 40
            :h-size-large 60
            :h-size-x-large 80
            :h-size-xx-large 100}})
