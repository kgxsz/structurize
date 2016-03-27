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


(def general
  [:html {:font-size (u/px 10)}

   [:body {:font-family "sans-serif"
           :font-size "1.5rem"
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
           :font-size "1.2rem"
           :line-height "1.7rem"
           :overflow :auto
           :white-space :nowrap}

   [:.init-page {:width (u/vw 100)
                 :height (u/vh 100)
                 :display :flex
                 :flex-direction :column
                 :justify-content :center
                 :font-size "1.5rem"
                 :align-items :center}
    [:.icon {:font-size "6rem"
             :margin-bottom "10px"}]]


   [:.home-page {:display :flex
                 :flex-direction :column
                 :justify-content :center
                 :height "100vh"}
    [:.me-context {:display :flex
                   :flex-direction :column
                   :justify-content :center
                   :align-items :center
                   :margin-bottom "20px"}

     [:.icon-mustache {:font-size "10rem"
                       :color (:white-b colours)}]
     [:.avatar {:width "100px"
                :height "100px"
                :background-color (c/rgba [0 10 20 0.2])
                :border-width "3px"
                :border-style :solid
                :border-radius "100px"}]
     [:.hero {:font-family "'Raleway', Arial"
              :color (:white-b colours)
              :font-size "4rem"
              :line-height 1
              :margin-top "20px"}]
     [:.sign-in-with-github {:margin-top "30px"}]
     [:.sign-out {:margin-top "20px"}]]

    [:.playground {:display :flex
                   :flex-direction :row
                   :justify-content :center
                   :align-items :center}
     [:.button {:margin "0 10px"}
      [:.button-icon {:margin-bottom "5px"}]
      [:.button-text {:font-size "1.7rem"
                      :font-weight :bold}]]]]


   [:.button {:display :flex
              :justify-content :center
              :align-items :center
              :min-width "50px"
              :height "45px"
              :padding "0 30px"
              :color (:white-b colours)
              :border-width "3px"
              :border-style :solid
              :border-radius "7px"
              :font-family "sans-serif"
              :font-size "1.5rem"
              :background-color (c/rgba [0 10 20 0.2])
              :line-height "1.7rem"}

    [:&:hover {:background-color (c/rgba [0 10 20 0.4])}]

    [:.button-icon {:font-size "2.2rem"
                    :margin-right "7px"}]]

   [:.tooling {:width "40vw"
               :height "100%"
               :min-width "320px"
               :min-height "320px"
               :background-color (c/rgba [0 10 20 0.7])
               :position :fixed
               :top 0
               :right 0}

    [:&.collapsed {:left "100vw"}]

    [:.tooling-tab {:display :flex
                    :justify-content :center
                    :align-items :center
                    :background-color (c/rgba [0 10 20 0.7])
                    :width "25px"
                    :height "33px"
                    :border-top-left-radius "5px"
                    :border-bottom-left-radius "5px"
                    :position :absolute
                    :top "15px"
                    :left "-25px"}
     [:.icon-cog {:color (:white-a colours)
                  :font-size "1.5rem"
                  :margin "0 0 1px 2px"}]]

    [:.browsers {:display :flex
                 :flex-direction :column
                 :width "100%"
                 :height "100%"
                 :font-family "'Fira Mono', monospace"
                 :font-size "1.2rem"
                 :color (:white-b colours)
                 :line-height "1.7rem"
                 :white-space :nowrap}

     [:.browser {:background-color (c/rgba [80 90 100 0.3])
                 :margin "15px 15px 0 15px"
                 :padding "10px"
                 :border-radius "5px"
                 :overflow :auto}
      [:&:last-child {:margin-bottom "15px"}]]

     [:.state-browser {:flex-grow 1
                       :height 0}

      [:.node {:display :flex}

       [:.node-brace {:padding-top "2px"}]

       [:.node-value :.node-key {:height "100%"
                                 :margin-bottom "2px"
                                 :padding "2px 3px 1px 3px"
                                 :border-radius "3px"
                                 :background-color (:grey-c colours)}]

       [:.node-key {:display :flex
                    :align-items :center
                    :margin-left "7px"
                    :margin-right "2px"}
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
                         :width "15px"
                         :height "15px"
                         :font-size "1.3rem"
                         :margin "0 1px"
                         :border-radius "3px"}

        [:&:first-child {:margin-left 0}]

        [:.icon {:padding-left "1px"}]

        [:&.cursored {:background-color (:dark-green colours)
                      :color (:white-a colours)}]
        [:&.mutated {:background-color (:dark-blue colours)
                     :color (:white-a colours)}]]]]]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; construction zone


    [:.event-browser {:height "150px"
                      :display :flex
                      :flex-direction :column
                      :justify-content :space-around
                      :align-items :flex-start}

     [:.events {:display :flex}]
     [:.event {:margin-right "5px"
               :padding "3px 5px"
               :border-radius "3px"
               :height "100%"
               :color (:dark-purple colours)
               :background-color (:light-purple colours)}]
     ]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

    ]])


(def main
  [["::-webkit-scrollbar" {:display :none}]
   meyer-reset
   general
   components])
