(ns structurize.styles.vars
  (:require [garden.color :as c]))

(def vars
  {:color {:tranparent (c/rgba [0 0 0 0])
           :black-a "#000000"
           :black-b "#000A14"
           :white-a "#FFFFFF"
           :white-b "#F2F2F2"
           :white-c "#DDDDDD"
           :white-d "#CCCCCC"
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

   :h-size {:xx-small 25
            :x-small 30
            :small 35
            :medium 40
            :large 60
            :x-large 80
            :xx-large 100}

   :spacing {:xx-small 2
             :x-small 6
             :small 8
             :medium 10
             :large 15
             :x-large 20
             :xx-large 30
             :xxx-large 50}

   :nudge {:small 1
           :medium 2
           :large 3
           :x-large 4
           :xx-large 6}

   :filling {:xxx-small 4
             :xx-small 10
             :x-small 16
             :small 22
             :medium 26
             :large 32
             :x-large 40
             :xx-large 60
             :xxx-large 90}

   :border-width {:small 1
                  :medium 2
                  :large 3}

   :border-radius {:x-small 2
                   :small 3
                   :medium 4
                   :large 5
                   :x-large 6
                   :xx-large 7}

   :button-width {:medium 220}

   :button-height {:medium 50}

   :avatar-width {:medium 130}

   :avatar-height {:medium 130}

   :header-height {:medium 50}
   :masthead-height {:medium 35}

   :hero-image-min-height {:medium 120}
   :hero-image-max-height {:medium 480}

   :transition-duration {:medium 400}

   :proportion {:x-small 25
                :small 30
                :medium 50
                :large 70}

   :alpha {:low 0.3
           :medium 0.5
           :high 0.7}})
