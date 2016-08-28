(ns structurize.styles.main
  (:require [garden.color :as c]
            [garden.units :as u]))


(defn alpha [hex alpha]
  (assoc (c/hex->rgb hex) :alpha alpha))


(def colours
  {:tranparent (c/rgba [0 0 0 0])
   :black-a "#000000"
   :black-b "#000A14"
   :white-a "#FFFFFF"
   :white-b "#F6F9FC"
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
   :spacing-x-small 5
   :spacing-small 8
   :spacing-medium 10
   :spacing-large 15
   :spacing-x-large 20
   :spacing-xx-large 30

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

   :avatar-width-medium 100

   :avatar-height-medium 100

   :transition-duration 200

   :proportion-x-small 25
   :proportion-small 30
   :proportion-medium 50
   :proportion-large 70

   :alpha-low 0.3
   :alpha-medium 0.5
   :alpha-high 0.7})


(def meyer-reset
  [[:html :body :div :span :applet :object :iframe :h1 :h2 :h3 :h4 :h5 :h6 :p
    :blockquote :pre :a :abbr :acronym :address :big :cite :code :del :dfn :em
    :img :ins :kbd :q :s :samp :small :strike :strong :sub :sup :tt :var :b :u
    :i :center :dl :dt :dd :ol :ul :li :fieldset :form :label :legend :table
    :caption :tbody :tfoot :thead :tr :th :td :article :aside :canvas :details
    :embed :figure :figcaption :footer :header :hgroup :menu :nav :output :ruby
    :section :summary :time :mark :audio :video
    {:margin 0 :padding 0 :border 0 :font-size (u/percent 100) :font :inherit :vertical-align :baseline}]
   [:* {:box-sizing :border-box}]
   [:article :aside :details :figcaption :figure :footer :header :hgroup :menu :nav :section
    {:display :block}]
   [:body {:line-height 1}]
   [:ol :ul {:list-style :none}]
   [:blockquote :q {:quotes :none}
    [:&:before :&:after {:content :none}]]
   [:table {:border-collapse :collapse :border-spacing 0}]])


(def general
  [:html
   [:body {:font-family "sans-serif"
           :font-size (-> v :p-size-medium u/px)
           :color (:white-a colours)
           :background-color (:white-a colours)
           :background-image "url(\"/images/blurred-background.jpg\")"
           :background-repeat :no-repeat
           :background-position [:center :center]
           :background-attachements :fixed
           :background-size :cover}]

   [:#js-root {:width (u/vw 100)
               :height (u/vh 100)}]])


(def states
  [:html
   [:.is-hidden {:display :none}]])


(def layouts
  [:html
   [:.l-underlay {:position :relative}]

   [:.l-overlay {:width (u/percent 100)
                 :height (u/percent 100)
                 :pointer-events :none
                 :position :absolute
                 :background-color (:tranparent colours)
                 :top 0
                 :left 0}
    [:&--fill-viewport {:width (u/vw 100)
                        :height (u/vh 100)
                        :position :fixed}]
    [:&__content {:pointer-events :auto}]]

   [:.l-row {:display :flex
             :flex-direction :row}
    [:&--height-100 {:height (u/percent 100)}]
    [:&--justify
     [:&-center {:justify-content :center}]]
    [:&__item
     [:&--grow {:flex-grow 1}]]]

   [:.l-col {:display :flex
             :flex-direction :column}
    [:&--fill-parent {:width (u/percent 100)
                      :height (u/percent 100)}]
    [:&--justify
     [:&-center {:justify-content :center}]]
    [:&--align
     [:&-center {:align-items :center}]
     [:&-end {:align-items :flex-end}]]
    [:&__item
     [:&--grow {:flex-grow 1}]]]

   [:.l-cell {:display :flex}
    [:&--fill-parent {:width (u/percent 100)
                      :height (u/percent 100)}]
    [:&--justify
     [:&-center {:justify-content :center}]]
    [:&--align
     [:&-center {:align-items :center}]
     [:&-end {:align-items :flex-end}]]]

   [:.l-spacing
    [:&--margin
     [:&-top
      [:&-small {:margin-top (-> v :spacing-small u/px)}]
      [:&-medium {:margin-top (-> v :spacing-medium u/px)}]]
     [:&-right
      [:&-small {:margin-right (-> v :spacing-small u/px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small u/px)}]]
     [:&-left
      [:&-small {:margin-left (-> v :spacing-small u/px)}]]]]

   [:.l-slide-over {:height (u/percent 100)
                    :background-color (:transparent colours)
                    :position :absolute
                    :top 0
                    :transition-property :right
                    :transition-duration (-> v :transition-duration u/ms)}]])


(def components
  [:html

   [:.c-icon
    [:&--h-size-xx-large {:font-size (-> v :h-size-xx-large u/px)}]
    [:&--h-size-large {:font-size (-> v :h-size-large u/px)}]]

   [:.c-image {:transition-property :opacity
               :transition-duration (-> v :transition-duration u/ms)}
    [:&--transparent {:opacity 0}]]

   [:.c-button {:width (-> v :button-width-medium u/px)
                :height (-> v :button-height-medium u/px)
                :color (:white-a colours)
                :background-color (alpha (:black-b colours) (:alpha-low v))
                :border-width (-> v :border-width-large u/px)
                :border-style :solid
                :border-color (:white-a colours)
                :padding 0
                :border-radius (-> v :border-radius-xx-large u/px)
                :outline :none
                :cursor :pointer
                :font-family "sans-serif"
                :font-size (-> v :p-size-large u/px)}
    [:&:hover {:background-color (alpha (:black-b colours) (:alpha-medium v))}]]

   [:.c-tooling {:width (u/percent 100)
                 :height (u/percent 100)
                 :padding (-> v :spacing-medium u/px)
                 :background-color (alpha (:black-b colours) (:alpha-high v))}

    [:&__handle {:display :flex
                 :justify-content :center
                 :align-items :center
                 :cursor :pointer
                 :background-color (alpha (:black-b colours) (:alpha-high v))
                 :width (-> v :filling-medium u/px)
                 :height (-> v :filling-medium u/px)
                 :border-top-left-radius (-> v :border-radius-medium u/px)
                 :border-bottom-left-radius (-> v :border-radius-medium u/px)
                 :position :absolute
                 :top (-> v :spacing-medium u/px)
                 :left (-> v :filling-medium - u/px)}]


    [:&__item {:width (u/percent 100)
               :padding (-> v :spacing-medium u/px)
               :background-color (alpha (:grey-a colours) (:alpha-low v))
               :border-radius (-> v :border-radius-medium u/px)
               :margin-bottom (-> v :spacing-medium u/px)
               :overflow :auto}

     ["&::-webkit-scrollbar" {:display :none}]

     [:&:last-child {:margin-bottom 0}]]]


   [:.c-writes-browser {:font-family "'Fira Mono', monospace"}

    [:&__controls {:background-color (alpha (:grey-a colours) (:alpha-low v))
                   :border-radius (-> v :border-radius-medium u/px)
                   :padding (-> v :spacing-medium u/px)}

     [:&__item {:display :flex
                :justify-content :center
                :align-items :center
                :width (-> v :filling-medium u/px)
                :height (-> v :filling-medium u/px)
                :margin-bottom (-> v :spacing-x-small u/px)
                :border-radius (-> v :filling-medium (/ 2) u/px)
                :font-size (-> v :p-size-small u/px)
                :opacity (:alpha-low v)}

      [:&:last-child {:margin-bottom 0}]

      [:&--green {:background-color (:light-green colours)
                  :color (:dark-green colours)}]

      [:&--yellow {:background-color (:light-yellow colours)
                   :color (:dark-yellow colours)}]

      [:&--opaque {:opacity 1}]

      [:&--clickable {:cursor :pointer}]]]

    [:&__item {:padding (-> v :spacing-medium u/px)
               :padding-right 0}

     [:&:last-child {:margin-right (-> v :spacing-x-large u/px)}]]

    [:&__pill-superscript {:display :flex
                           :align-items :flex-end
                           :height (-> v :filling-medium u/px)
                           :padding-left (-> v :spacing-small u/px)
                           :padding-bottom (-> v :spacing-xx-small u/px)
                           :font-size (-> v :p-size-small u/px)}
     [:&__symbol {:margin-right (-> v :spacing-xx-small u/px)}]]

    [:&__pill {:padding (-> v :spacing-x-small u/px)
               :border-style :dotted
               :border-width (-> v :border-width-medium u/px)
               :border-radius (-> v :border-radius-large u/px)}

     [:&__content {:display :flex
                   :align-items :center
                   :height (-> v :filling-small u/px)
                   :padding-left (-> v :spacing-x-small u/px)
                   :padding-right (-> v :spacing-x-small u/px)
                   :border-radius (-> v :border-radius-small u/px)
                   :background-color (:light-green colours)
                   :font-size (-> v :p-size-x-small u/px)
                   :white-space :nowrap
                   :color (:dark-green colours)}]]]


   [:.c-app-browser {:font-family "'Fira Mono', monospace"
                     :font-size (-> v :p-size-small u/px)}

    [:&__brace {:padding-top (-> v :nudge-xx-large u/px)}]

    [:&__node {:display :flex}

     [:&__value :&__key {:height (-> v :filling-small u/px)
                         :font-size (-> v :p-size-x-small u/px)
                         :padding-left (-> v :spacing-x-small u/px)
                         :padding-right (-> v :spacing-x-small u/px)
                         :border-radius (-> v :border-radius-small u/px)
                         :margin-bottom (-> v :spacing-xx-small u/px)
                         :background-color (:grey-c colours)
                         :white-space :nowrap}]

     [:&__key {:display :flex
               :align-items :center
               :margin-left (-> v :spacing-small u/px)
               :margin-right (-> v :spacing-xx-small u/px)
               :cursor :pointer}
      [:&--first {:margin-left 0}]
      [:&--written {:color (:light-green colours)}]
      [:&--downstream-focused {:background-color (:light-blue colours)
                               :color (:dark-blue colours)}]
      [:&--focused {:background-color (:dark-blue colours)
                    :color (:light-blue colours)}]]

     [:&__value {:display :flex
                :align-items :center}
      [:&--clickable {:cursor :pointer}]
      [:&--written {:color (:light-green colours)}]
      [:&--focused {:background-color (:light-blue colours)
                    :color (:dark-blue colours)}]]]]

   [:.c-page {:overflow :auto
              :min-height (u/vh 100)}]

   [:.c-hero {:margin-top (-> v :proportion-x-small u/vh)
              :margin-bottom (-> v :spacing-xx-large u/px)
              :text-align :center}
    [:&__caption {:font-family "'Raleway', Arial"
                  :font-size (-> v :h-size-x-small u/px)
                  :margin-top (-> v :spacing-x-large u/px)}]
    [:&__avatar {:width (-> v :avatar-width-medium u/px)
                 :height (-> v :avatar-height-medium u/px)
                 :z-index 0;
                 :background-color (alpha (:black-b colours) (:alpha-low v))
                 :border-style :solid
                 :overflow :hidden
                 :border-width (-> v :border-width-large u/px)
                 :border-radius (-> v :avatar-height-medium u/px)}]
    [:&__inter-icon {:font-size (-> v :h-size-large u/px)
                     :margin-top (-> v :spacing-large u/px)
                     :margin-left (-> v :spacing-x-large u/px)
                     :margin-right (-> v :spacing-x-large u/px)}]]])


(def main
  [meyer-reset
   general
   states
   layouts
   components])
