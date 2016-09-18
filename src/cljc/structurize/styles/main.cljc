(ns structurize.styles.main
  (:require [structurize.styles.vars :refer [vars]]
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
            [structurize.styles.layouts.cell :refer [cell]]
            [structurize.styles.layouts.row :refer [row]]
            [structurize.styles.layouts.col :refer [col]]
            [structurize.styles.layouts.underlay :refer [underlay]]
            [structurize.styles.layouts.overlay :refer [overlay]]
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
           :font-size (-> vars :p-size :medium px)
           :color (-> :color :grey-a)
           :background-color (-> :color :white-b)}]

   [:#js-root {:width (vw 100)
               :height (vh 100)}]])


(def states
  [:html
   [:.is-hidden {:display :none}]
   [:.is-transparent {:opacity 0}]])


(def layouts
  [:html
   cell
   row
   col
   underlay
   overlay])


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
