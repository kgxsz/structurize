(ns structurize.styles.main
  (:require [structurize.styles.variables :refer [v colours]]
            [structurize.styles.utils :as u]
            [structurize.styles.components.header :refer [header]]
            [structurize.styles.components.hero :refer [hero]]
            [structurize.styles.components.masthead :refer [masthead]]
            [structurize.styles.components.image :refer [image]]
            [structurize.styles.components.slide-over :refer [slide-over]]
            [structurize.styles.components.writes-browser :refer [writes-browser]]
            [structurize.styles.components.app-browser :refer [app-browser]]
            [structurize.styles.components.tooling :refer [tooling]]
            [structurize.styles.components.page :refer [page]]
            [structurize.styles.components.text :refer [text]]
            [structurize.styles.components.icon :refer [icon]]
            [garden.color :as c]
            [garden.units :refer [px percent ms vh vw]]))

(def meyer-reset
  [[:html :body :div :span :applet :object :iframe :h1 :h2 :h3 :h4 :h5 :h6 :p
    :blockquote :pre :a :abbr :acronym :address :big :cite :code :del :dfn :em
    :img :ins :kbd :q :s :samp :small :strike :strong :sub :sup :tt :var :b :u
    :i :center :dl :dt :dd :ol :ul :li :fieldset :form :label :legend :table
    :caption :tbody :tfoot :thead :tr :th :td :article :aside :canvas :details
    :embed :figure :figcaption :footer :header :hgroup :menu :nav :output :ruby
    :section :summary :time :mark :audio :video
    {:margin 0 :padding 0 :border 0 :font-size (percent 100) :font :inherit :vertical-align :baseline}]
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
   [:body {:font-family "'Raleway', Arial"
           :font-size (-> v :p-size-medium px)
           :color (:grey-a colours)
           :background-color (:white-b colours)}]

   [:#js-root {:width (vw 100)
               :height (vh 100)}]])


(def states
  [:html
   [:.is-hidden {:display :none}]
   [:.is-transparent {:opacity 0}]])


(def layouts
  [:html
   [:.l-underlay {:position :relative}]

   [:.l-overlay {:width (percent 100)
                 :height (percent 100)
                 :pointer-events :none
                 :position :absolute
                 :background-color (:tranparent colours)
                 :z-index 1
                 :top 0
                 :left 0}
    [:&--fill-viewport {:width (vw 100)
                        :height (vh 100)
                        :position :fixed}]
    [:&__content {:pointer-events :auto}]]

   [:.l-row {:display :flex
             :flex-direction :row}
    [:&--fill-parent {:width (percent 100)
                      :height (percent 100)}]
    [:&--height-100 {:height (percent 100)}]
    [:&--width-100 {:width (percent 100)}]
    [:&--justify
     [:&-center {:justify-content :center}]
     [:&-space-between {:justify-content :space-between}]
     [:&-start {:justify-content :flex-start}]
     [:&-end {:justify-content :flex-end}]]
    [:&__item
     [:&--grow {:flex-grow 1}]]]

   [:.l-col {:display :flex
             :flex-direction :column}
    [:&--fill-parent {:width (percent 100)
                      :height (percent 100)}]
    [:&--height-100 {:height (percent 100)}]
    [:&--width-100 {:width (percent 100)}]
    [:&--justify
     [:&-center {:justify-content :center}]
     [:&-space-between {:justify-content :space-between}]
     [:&-start {:justify-content :flex-start}]
     [:&-end {:justify-content :flex-end}]]
    [:&--align
     [:&-center {:align-items :center}]
     [:&-start {:align-items :flex-start}]
     [:&-end {:align-items :flex-end}]]
    [:&--margin
     [:&-top
      [:&-small {:margin-top (-> v :spacing-small px)}]
      [:&-medium {:margin-top (-> v :spacing-medium px)}]
      [:&-xxx-large {:margin-top (-> v :spacing-xxx-large px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]
      [:&-medium {:margin-bottom (-> v :spacing-medium px)}]]
     [:&-right
      [:&-small {:margin-right (-> v :spacing-small px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]]
     [:&-left
      [:&-small {:margin-left (-> v :spacing-small px)}]]]
    [:&__item
     [:&--grow {:flex-grow 1}]]]

   [:.l-cell
    [:&--fill-parent {:width (percent 100)
                      :height (percent 100)}]
    [:&--height-100 {:height (percent 100)}]
    [:&--width-100 {:width (percent 100)}]
    [:&--justify
     [:&-center {:display :flex
                 :flex-direction :row
                 :justify-content :center}]
     [:&-space-between {:display :flex
                        :flex-direction :row
                        :justify-content :space-between}]
     [:&-start {:display :flex
                :flex-direction :row
                :justify-content :flex-start}]
     [:&-end {:display :flex
              :flex-direction :row
              :justify-content :flex-end}]]
    [:&--align
     [:&-center {:display :flex
                 :flex-direction :row
                 :align-items :center}]
     [:&-end {:display :flex
              :flex-direction :row
              :align-items :flex-end}]]
    [:&--margin
     [:&-top
      [:&-small {:margin-top (-> v :spacing-small px)}]
      [:&-medium {:margin-top (-> v :spacing-medium px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]
      [:&-medium {:margin-bottom (-> v :spacing-medium px)}]]
     [:&-right
      [:&-small {:margin-right (-> v :spacing-small px)}]
      [:&-medium {:margin-right (-> v :spacing-medium px)}]
      [:&-large {:margin-right (-> v :spacing-large px)}]]
     [:&-bottom
      [:&-small {:margin-bottom (-> v :spacing-small px)}]]
     [:&-left
      [:&-small {:margin-left (-> v :spacing-small px)}]
      [:&-medium {:margin-left (-> v :spacing-medium px)}]
      [:&-large {:margin-left (-> v :spacing-large px)}]]]]])


(def components
  [:html
   header
   hero
   masthead
   image
   slide-over
   page
   text
   icon
   tooling
   writes-browser
   app-browser])

(def main
  [meyer-reset
   general
   states
   layouts
   components])
