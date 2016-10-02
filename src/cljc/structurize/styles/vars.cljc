(ns structurize.styles.vars
  (:require [garden.color :as c]))

(def vars
  {:color (merge
           c/color-name->hex
           {:deepgrey "#343337"
            :palegreen "#D3EDA3"
            :dullblue "#5992AA"})

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

   :filling {:xx-small 10
             :x-small 16
             :small 22
             :medium 26
             :large 32
             :x-large 40
             :xx-large 60}

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

   :proportion {:0 0
                :10 10
                :20 20
                :25 25
                :30 30
                :40 40
                :50 50
                :60 60
                :70 70
                :75 75
                :80 80
                :90 90
                :100 100}

   :alpha {:none 0
           :low 0.3
           :medium 0.5
           :high 0.7}})
