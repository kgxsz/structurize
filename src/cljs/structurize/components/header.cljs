(ns structurize.components.header
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.triptych :refer [triptych]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header-left [Φ {:keys [width col-n col-width gutter margin-left]}]
  [:div.c-header__item {:style {:width (- width gutter)
                                :margin-left (+ margin-left gutter)}}])


(defn header-center [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
  [:div.c-header__item  {:style {:width (- width gutter gutter)
                                 :margin-left (+ margin-left gutter)
                                 :margin-right (+ margin-right gutter)}}])


(defn header-right [Φ {:keys [width col-n col-width gutter margin-right]}]
  [:div.c-header__item  {:style {:width (- width gutter)
                                 :margin-right (+ margin-right gutter)}}])


(defn header [Φ]
  (log-debug Φ "render header")
  [:div.c-header
   [triptych Φ {:left {:hidden #{:xs :sm :md}
                       :c [header-left]}
                :center {:c [header-center]}
                :right {:hidden #{:xs :sm :md}
                        :c [header-right]}}]])
