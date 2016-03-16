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
           :position :relative}])

(def tooling
  [:.tooling {:width (u/vw 50)
              :height (u/vh 100)
              :background-color "#272B30"
              :color "#DDD"
              :font-size (u/rem 1.2)
              :position :fixed
              :top 0
              :right 0}

   [:.state-browser {:font-family "monospace"
                     :margin (u/rem 1.5)
                     :padding (u/rem 1)
                     :background-color "#343337"
                     :border-radius (u/px 5)
                     :white-space :nowrap}

    [:.nodes-container {:display :flex}

     [:.node-key :.node-value {:background-color "#474C51"
                               :margin-bottom (u/rem 0.2)
                               :padding-left (u/rem 0.3)
                               :padding-right (u/rem 0.3)
                               :padding-bottom (u/rem 0.1)
                               :border-radius (u/px 3)}
      [:&:hover {:cursor :default}]]

     [:&.focused
      [:.node-key :.node-value {:background-color "#2478BD"}]]

     [:.node {:display :flex}

      [:.node-key {:margin-right (u/rem 0.2)
                   :display :flex
                   :align-items :center}

       [:&.focused {:background-color "#3E6733"}]

       [:.node-key-flags {:display :flex
                          :margin-right (u/rem 0.1)}
        [:.node-key-flag {:display :none
                          :width (u/rem 0.9)
                          :height (u/rem 0.9)
                          :border-radius (u/rem 0.2)
                          :margin-left (u/rem 0.2)}
         [:&.cursored {:display :block
                       :background-color "#F3C96E"}]]]]

      [:.node-value
       [:&.focused {:background-color "#2478BD"}]]]]]])


(def main
  [meyer-reset
   html
   body
   root
   tooling])
