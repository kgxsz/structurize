(ns structurize.styles.main
  (:require [garden.units :as u]
            [garden.color :as c]
            [garden.selectors :as s]))


(def colours
  {:tinted-black-a "#000204"
   :tinted-black-b "#101214"
   :white-a "#FFFFFF"
   :white-b "#F6F9FC"
   :white-c "#DDDDDD"
   :grey-a "#272B30"
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


(def meyer-reset
  [[:html :body :div :span :applet :object :iframe :h1 :h2 :h3 :h4 :h5 :h6 :p
    :blockquote :pre :a :abbr :acronym :address :big :cite :code :del :dfn :em
    :img :ins :kbd :q :s :samp :small :strike :strong :sub :sup :tt :var :b :u
    :i :center :dl :dt :dd :ol :ul :li :fieldset :form :label :legend :table
    :caption :tbody :tfoot :thead :tr :th :td :article :aside :canvas :details
    :embed :figure :figcaption :footer :header :hgroup :menu :nav :output :ruby
    :section :summary :time :mark :audio :video
    {:margin 0 :padding 0 :border 0 :font-size (u/percent 100) :font :inherit :vertical-align :baseline}]
   [:article :aside :details :figcaption :figure :footer :header :hgroup :menu :nav :section
    {:display :block}]
   [:body {:line-height 1}]
   [:ol :ul {:list-style :none}]
   [:blockquote :q {:quotes :none}
    [:&:before :&:after {:content :none}]]
   [:table {:border-collapse :collapse :border-spacing 0}]])


(def general
  [:html {:font-size (u/px 10)}

   [:body {:font-family "sans-serif"
           :font-size (u/rem 1.5)
           :line-height 1.5
           :color (:white-b colours)
           :background-color (:white-b colours)
           :background-image "url(\"/images/blurred-background-b.jpg\")"
           :background-repeat :no-repeat
           :background-position [:center :center]
           :background-attachements :fixed
           :background-size :cover}]

   [:.clickable:hover {:cursor :pointer}]
   [:.hidden {:display :none}]])


(def components
  [:#root {:width (u/vw 100)
           :height (u/vh 100)
           :position :relative
           :min-width (u/px 320)
           :min-height (u/px 320)
           :font-size (u/rem 1.2)
           :line-height (u/rem 1.7)
           :overflow :auto
           :white-space :nowrap}

   [:.init-page {:width (u/vw 100)
                 :height (u/vh 100)
                 :display :flex
                 :flex-direction :column
                 :justify-content :center
                 :font-size (u/rem 1.5)
                 :align-items :center}
    [:.icon {:font-size (u/rem 6)
             :margin-bottom (u/rem 1)}]]


   [:.home-page {:width (u/vw 100)
                 :height (u/vh 100)
                 :display :flex
                 :flex-direction :column
                 :justify-content :center
                 :align-items :center}
    [:.icon-mustache {:font-size (u/rem 10)
                      :color (:white-b colours)}]
    [:.avatar {:width (u/rem 10)
               :height (u/rem 10)
               :background-color (c/rgba [0 10 20 0.2])
               :border-width (u/rem 0.3)
               :border-style :solid
               :border-radius (u/rem 10)}]
    [:.hero {:font-family "'Raleway', Arial"
             :color (:white-b colours)
             :font-size (u/rem 4)
             :line-height 1
             :margin-top (u/rem 2)}]
    [:.sign-in-with-github {:margin-top (u/rem 3)}]
    [:.sign-out {:margin-top (u/rem 2)}]]


   [:.button {:display :flex
              :justify-content :center
              :align-items :center
              :min-width (u/rem 12)
              :height (u/rem 4.5)
              :padding-left (u/rem 1)
              :padding-right (u/rem 1)
              :color (:white-b colours)
              :border-width (u/rem 0.3)
              :border-style :solid
              :border-radius (u/px 7)
              :font-family "sans-serif"
              :font-size (u/rem 1.5)
              :background-color (c/rgba [0 10 20 0.2])
              :line-height (u/rem 1.7)}

    [:&:hover {:background-color (c/rgba [0 10 20 0.4])}]

    [:.button-icon {:font-size (u/rem 2.2)
                    :margin-right (u/rem 0.7)}]]

   [:.tooling {:width (u/rem 90)
               :height (u/percent 100)
               :min-width (u/px 320)
               :min-height (u/px 320)
               :background-color (c/rgba [0 10 20 0.7])
               :position :fixed
               :top 0
               :right 0}

    [:&.collapsed {:right (u/rem -90)}]

    [:.tooling-tab {:display :flex
                    :justify-content :center
                    :align-items :center
                    :background-color (c/rgba [0 10 20 0.7])
                    :width (u/rem 2.5)
                    :height (u/rem 3.3)
                    :border-top-left-radius (u/px 5)
                    :border-bottom-left-radius (u/px 5)
                    :position :absolute
                    :top (u/rem 1.5)
                    :left (u/rem -2.5)}
     [:.icon-cog {:color (:white-a colours)
                  :font-size (u/rem 1.5)
                  :margin-bottom (u/rem 0.1)
                  :margin-left (u/rem 0.2)}]]

    [:.browsers {:display :flex
                 :flex-direction :column
                 :width (u/rem 90)
                 :height (u/vh 100)
                 :font-family "'Fira Mono', monospace"
                 :font-size (u/rem 1.2)
                 :color (:white-b colours)
                 :line-height (u/rem 1.7)
                 :white-space :nowrap}

     [:.state-browser {:flex-grow 1
                       :height 0
                       :background-color (c/rgba [80 90 100 0.3])
                       :margin (u/rem 1.5)
                       :padding (u/rem 1)
                       :border-radius (u/px 5)
                       :overflow :auto}

      [:.node {:display :flex}

       [:.node-brace {:padding-top (u/rem 0.2)}]

       [:.node-value :.node-key {:height (u/percent 100)
                                 :margin-bottom (u/rem 0.2)
                                 :padding-top (u/rem 0.2)
                                 :padding-bottom (u/rem 0.1)
                                 :padding-right (u/rem 0.3)
                                 :padding-left (u/rem 0.3)
                                 :border-radius (u/px 3)
                                 :background-color (:grey-c colours)}]

       [:.node-key {:display :flex
                    :align-items :center
                    :margin-left (u/rem 0.7)
                    :margin-right (u/rem 0.2)}
        [:span {:pointer-events :none}]
        [:&.first {:margin-left 0}]
        [:&.focused :&.upstream-focused {:background-color (:light-blue colours)
                                         :color (:dark-blue colours)}]]

       [:.node-value
        [:&.focused {:background-color (:light-green colours)
                     :color (:dark-green colours)}]]

       [:.node-group
        [:&.focused
         [:.node-key :.node-value {:background-color (:light-green colours)
                                   :color (:dark-green colours)}]]]

       [:.node-key-flag {:display :flex
                         :justify-content :center
                         :align-items :center
                         :width (u/rem 1.5)
                         :height (u/rem 1.5)
                         :font-size (u/rem 1.3)
                         :margin-left (u/rem 0.1)
                         :margin-right (u/rem 0.1)
                         :border-radius (u/rem 0.3)}
        [:.icon {:padding-left (u/rem 0.1)}]

        [:&.cursored {:background-color (:dark-green colours)
                      :color (:white-a colours)}]
        [:&.mutated {:background-color (:dark-blue colours)
                     :color (:white-a colours)}]]]]]

    [:.event-browser {:height (u/rem 15)
                      :margin (u/rem 1.5)
                      :background-color (c/rgba [80 90 100 0.3])
                      :margin-top 0
                      :border-radius (u/px 5)}]]])

(def main
  [meyer-reset
   general
   components])
