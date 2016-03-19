(ns structurize.styles.main
  (:require [garden.units :as u]
            [garden.color :as c]))

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


(def html
  [:html {:font-size (u/px 10)}])


(def body
  [:body {:font-size (u/px 10)
          :line-height 1.5
          :background-image "url(\"/images/blurred-background.jpg\")"
          :background-repeat :no-repeat
          :background-position [:center :center]
          :background-attachements :fixed
          :background-size :cover}])


(def root
  [:#root {:width (u/vw 100)
           :height (u/vh 100)
           :position :relative
           :min-width (u/px 320)
           :min-height (u/px 320)}])

(def colours
  {:tinted-black-a "#000204"
   :tinted-black-b "#101214"
   :white-a "#FFFFFF"
   :white-b "#DDDDDD"
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


(def tooling
  (let [state-browser {:flex-grow 1
                       :height 0
                       :margin (u/rem 1.5)
                       :padding (u/rem 1)
                       :border-radius (u/px 5)}
        event-browser {:height (u/rem 15)
                       :margin (u/rem 1.5)
                       :margin-top 0
                       :border-radius (u/px 5)}]

    [:.tooling {:width (u/rem 90)
                :height (u/percent 100)
                :min-width (u/px 320)
                :min-height (u/px 320)
                :position :fixed
                :top 0
                :right 0}

     [:&.collapsed {:right (u/rem -90)}]

     [:.tooling-tab {:display :flex
                     :justify-content :center
                     :align-items :center
                     :background-color (:tinted-black-a colours)
                     :opacity 0.8
                     :width (u/rem 2.5)
                     :height (u/rem 3.3)
                     :border-top-left-radius (u/px 5)
                     :border-bottom-left-radius (u/px 5)
                     :position :absolute
                     :top (u/rem 1.5)
                     :left (u/rem -2.5)}
      [:&:hover {:cursor :pointer}]
      [:.lnr-cog {:color (:white-a colours)
                  :font-size (u/rem 1.5)
                  :margin-bottom (u/rem 0.1)
                  :margin-left (u/rem 0.2)}]]

     [:.browsers-backing {:display :flex
                          :flex-direction :column
                          :width (u/percent 100)
                          :height (u/percent 100)
                          :background-color (:tinted-black-a colours)
                          :opacity 0.8
                          :position :absolute
                          :top 0
                          :right 0}

      [:.state-browser-backing (assoc state-browser :background-color (:tinted-black-b colours))]

      [:.event-browser-backing (assoc event-browser :background-color (:tinted-black-b colours))]]

     [:.browsers {:display :flex
                  :flex-direction :column
                  :width (u/rem 90)
                  :height (u/vh 100)
                  :color (:white-b colours)
                  :position :relative}

      [:.state-browser (merge state-browser
                              {:font-family "'Fira Mono', monospace"
                               :font-size (u/rem 1.2)
                               :line-height (u/rem 1.7)
                               :overflow :auto
                               :white-space :nowrap})

       [:.nodes-container {:display :flex}

        [:.braces {:padding-top (u/rem 0.2)}]

        [:.collapsed-value {:display :flex}
         [:div {:width (u/rem 0.4)
                :height (u/rem 0.4)
                :margin-left (u/rem 0.1)
                :margin-right (u/rem 0.1)
                :border-radius (u/rem 0.2)
                :background-color (:white-b colours)}]]

        [:.node-key :.node-value {:background-color (:grey-c colours)
                                  :margin-bottom (u/rem 0.2)
                                  :padding-top (u/rem 0.1)
                                  :padding-left (u/rem 0.3)
                                  :padding-right (u/rem 0.3)
                                  :padding-bottom (u/rem 0.1)
                                  :border-radius (u/px 3)}
         [:&:hover {:cursor :pointer}]]

        [:&.focused
         [:.collapsed-value
          [:div {:background-color (:dark-green colours)}]]

         [:.node-key :.node-value {:background-color (:light-green colours)
                                   :color (:dark-green colours)}]]

        [:.node {:display :flex}

         [:.node-key {:margin-right (u/rem 0.2)
                      :display :flex
                      :align-items :center}

          [:&.focused {:background-color (:light-blue colours)
                       :color (:dark-blue colours)}]

          [:.node-key-flags {:display :flex
                             :margin-right (u/rem 0.2)}

           [:.node-key-flag {:display :none
                             :width (u/rem 0.9)
                             :height (u/rem 0.9)
                             :border-radius (u/rem 0.2)
                             :margin-left (u/rem 0.2)}
            [:&.cursored {:display :block
                          :background-color (:light-purple colours)}]]]]

         [:.node-value {:display :flex
                        :align-items :center}

          [:&.focused {:background-color (:light-green colours)
                       :color (:dark-green colours)}
           [:.collapsed-value
            [:div {:background-color (:dark-green colours)}]]]]]]]]

     [:.event-browser (merge event-browser {})]]))


(def main
  [meyer-reset
   html
   body
   root
   tooling])
