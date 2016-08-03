(ns structurize.styles.main
  (:require [garden.color :as c]
            [garden.units :as u]))


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


(def v
  {:spacing 10
   :filling 30})

(def layouts
  [:#root

   [:.l-box
    [:&--height-100 {:height (u/percent 100)}]]

   [:.l-underlay {:position :relative}]

   [:.l-overlay {:width "100%"
                 :height "100%"
                 :pointer-events :none
                 :position :absolute
                 :background-color (c/rgba [0 0 0 0])
                 :top 0
                 :left 0}

    [:&--viewport-fixed {:width "100vw"
                         :height "100vh"
                         :position :fixed}]

    [:&__content {:pointer-events :auto}]]

   [:.l-row {:display :flex
             :flex-direction :row}
    [:&__item
     [:&--grow {:flex-grow 1}]]]

   [:.l-col {:display :flex
             :flex-direction :column}
    [:&__item
     [:&--grow {:flex-grow 1}]]]

   [:.l-cell {:display :flex}
    [:&--center {:justify-content :center
                 :align-items :center}]
    [:&--left {:align-items :center}]
    [:&--bottom-left {:align-items :flex-end}]]

   [:.l-slide-over {:height "100%"
                    :background-color (c/rgba [0 0 0 0])
                    :position :absolute
                    :top 0
                    :transition "right 0.2s"}]])


(def components
  [:#root

   [:.c-icon]

   [:.c-tooling {:box-sizing :border-box
                 :width (u/percent 100)
                 :height (u/percent 100)
                 :padding (-> v :spacing u/px)
                 :background-color (c/rgba [0 10 20 0.7])}

    [:&__handle {:cursor :pointer
                 :background-color (c/rgba [0 10 20 0.7])
                 :width "26px"
                 :height "26px"
                 :border-top-left-radius "5px"
                 :border-bottom-left-radius "5px"
                 :position :absolute
                 :top (-> v :spacing u/px)
                 :left "-26px"}]


    [:&__item {:box-sizing :border-box
               :width (u/percent 100)
               :padding (-> v :spacing u/px)
               :background-color (c/rgba [80 90 100 0.3])
               :border-radius "5px"
               :margin-bottom (-> v :spacing u/px)
               :overflow :auto}

     ["&::-webkit-scrollbar" {:display :none}]

     [:&:last-child {:margin-bottom 0}]]]


   [:.c-writes-browser {:font-family "'Fira Mono', monospace"
                        :line-height "1.7rem"
                        :font-size "1.2rem"}

    [:&__controls {:background-color (c/rgba [80 90 100 0.3])
                   :border-radius "5px"
                   :padding (-> v :spacing u/px)}


     [:&__item {:width (u/px 26)
                :height (u/px 26)
                :margin-bottom (u/px 5)
                :border-radius "13px"
                :opacity 0.3}

      [:&:last-child {:margin-bottom 0}]

      [:&--green {:background-color (:light-green colours)
                  :color (:dark-green colours)}]

      [:&--yellow {:background-color (:light-yellow colours)
                   :color (:dark-yellow colours)}]

      [:&--opaque {:opacity 1}]

      [:&--clickable {:cursor :pointer}]]]

    [:&__item {:padding (-> v :spacing u/px)
               :padding-right 0}

     [:&:last-child {:margin-right "20px"}]]

    [:&__pill-superscript {:box-sizing :border-box
                           :height (u/px 26)
                           :padding-bottom "5px"
                           :padding-left (u/px (+ 5 2))}
     [:&__symbol {:font-size "1.6rem"
                  :margin-right "2px"}]]

    [:&__pill-container {:box-sizing :border-box
                         :height (u/px (+ 26 10))
                         :padding (u/px 5)
                         :border "dotted 2px"
                         :border-radius "5px"}]

    [:&__pill {:height "100%"
               :padding-left (u/px 5)
               :padding-right (u/px 5)
               :border-radius "3px"
               :background-color (:light-green colours)
               :white-space :nowrap
               :color (:dark-green colours)}]]


   [:.c-app-browser {:font-family "'Fira Mono', monospace"
                     :font-size "1.2rem"
                     :white-space :nowrap
                     :line-height "1.7rem"}

    [:&__brace {:padding-top "2px"}]

    [:&__node {:display :flex}

     [:&__value :&__key {:height "100%"
                         :margin-bottom "2px"
                         :padding "2px 3px 1px 3px"
                         :border-radius "4px"
                         :background-color (:grey-c colours)}]

     [:&__key {:display :flex
               :align-items :center
               :margin-left "7px"
               :margin-right "2px"}
      [:span {:pointer-events :none}]
      [:&--first {:margin-left 0}]
      [:&--written :&--upstream-written {:color (:light-green colours)}]
      [:&--focused :&--upstream-focused {:background-color (:dark-blue colours)
                                         :color (:light-blue colours)}]]

     [:&__value
      [:&--written :&--upstream-written {:color (:light-green colours)}]
      [:&--focused {:background-color (:light-blue colours)
                   :color (:dark-blue colours)}]]]

    [:&__node-group
     ;; TODO this is a bit of a violation of BEM concerns
     [:&--focused
      [:c-app-browser__node__key :c-app-browser__node__value {:background-color (:light-blue colours)
                                                              :color (:dark-blue colours)}]]]

    ]
   ])

(def general
  [:html {:font-size (u/px 10)}

   [:body {:font-family "sans-serif"
           :font-size "1.4rem"
           :line-height 1.5
           :color (:white-b colours)
           :background-color (:white-b colours)
           :background-image "url(\"/images/blurred-background.jpg\")"
           :background-repeat :no-repeat
           :background-position [:center :center]
           :background-attachements :fixed
           :background-size :cover}]

   [:h1 {:font-size "3rem"}]
   [:h5 {:font-size "1.6rem"}]

   [:.clickable:hover {:cursor :pointer}]
   [:.hidden {:display :none}]])


(def pages
  [:#root
   [:.page {:display :flex
            :flex-direction :column
            :align-items :center}]])


(def components*
  [:#root
   [:.loading {:display :flex
               :flex-direction :column
               :justify-content :center
               :align-items :center
               :width (u/vw 100)
               :height (u/vh 100)}
    [:.icon-coffee-cup {:height "60px"
                        :font-size "6rem"}]
    [:.loading-caption {:margin-top "10px"}]]

   [:.hero {:margin-top "20vh"
            :text-align :center}
    [:.hero-visual {:display :flex
                    :justify-content :center
                    :align-items :center
                    :min-height "120px"}
     [:.hero-visual-divider {:font-size "4rem"
                             :margin "0 15px"}]
     [:.icon {:font-size "10rem"}]]
    [:.hero-caption {:font-family "'Raleway', Arial"
                     :margin "5px 0"}]]

   [:.options-section {:margin-top "30px"}
    [:.button {:margin-bottom "10px"}]]

   [:.large-avatar {:width "100px"
                    :height "100px"
                    :background-color (c/rgba [0 10 20 0.2])
                    :border "3px solid"
                    :border-radius "100px"}]

   [:.button {:display :flex
              :justify-content :center
              :align-items :center
              :min-width "50px"
              :height "45px"
              :padding "0 30px"
              :color (:white-b colours)
              :border "3px solid"
              :border-radius "7px"
              :font-family "sans-serif"
              :font-size "1.5rem"
              :background-color (c/rgba [0 10 20 0.2])
              :line-height "1.7rem"}

    [:&:hover {:background-color (c/rgba [0 10 20 0.4])}]

    [:.button-icon {:font-size "2.2rem"
                    :margin-right "7px"}]]


   [:.node {:display :flex}

    [:.node-brace {:padding-top "2px"}]

    [:.node-value :.node-key {:height "100%"
                              :margin-bottom "2px"
                              :padding "2px 3px 1px 3px"
                              :border-radius "4px"
                              :background-color (:grey-c colours)}]

    [:.node-key {:display :flex
                 :align-items :center
                 :margin-left "7px"
                 :margin-right "2px"}
     [:span {:pointer-events :none}]
     [:&.first {:margin-left 0}]
     [:&.written :&.upstream-written {:color (:light-green colours)}]
     [:&.focused :&.upstream-focused {:background-color (:dark-blue colours)
                                      :color (:light-blue colours)}]]

    [:.node-value
     [:&.written :&.upstream-written {:color (:light-green colours)}]
     [:&.focused {:background-color (:light-blue colours)
                  :color (:dark-blue colours)}]]

    [:.node-group
     [:&.written
      [:.node-key :.node-value {:color (:light-green colours)}]]
     [:&.focused
      [:.node-key :.node-value {:background-color (:light-blue colours)
                                :color (:dark-blue colours)}]]]]

   ])


(def main
  [meyer-reset
   general
   layouts
   components

   pages
   components*])
