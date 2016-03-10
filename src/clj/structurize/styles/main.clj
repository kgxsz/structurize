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
  [:body {:font-family "monospace"
          :font-size (u/px 10)
          :line-height 1.5
          :background-color (c/color-name->hex :indianred)}])


(def root
  [:div#root {:width (u/vw 100)
              :height (u/vh 100)
              :position :relative}])

(def event-state
  [:div.event-state {:width (u/vw 50)
                     :height (u/vh 100)
                     :background-color (c/color-name->hex :lavender)
                     :position :fixed
                     :top 0
                     :right 0}

   [:div.state {:overflow :auto
                :background-color (c/color-name->hex :skyblue)}

    [:div.opening-brace {:float :left}]

    [:div.keys-values {:float :left}]]])

(def main
  [meyer-reset
   html
   body
   root
   event-state])
