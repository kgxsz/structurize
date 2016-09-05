(ns structurize.components.hero
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.triptych :refer [triptych]]
            [structurize.components.image :refer [image]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


(def images ["images/hero-1.png" "images/hero-2.png" "images/hero-3.png"
             "images/hero-4.png" "images/hero-5.png" "images/hero-6.png"
             "images/hero-7.png" "images/hero-8.png" "images/hero-9.png"])


;; model ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +image (in [:hero :background-image]))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hero-center [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
  (let [src (rand-nth images)]
    [:div {:style {:width (+ width margin-left margin-right)}}
     [image Φ {:+image +image
               :src src}]]))


(defn hero [Φ]
  (log-debug Φ "render hero")
  [:div.c-hero
   [triptych Φ {:center {:c hero-center}}]])
