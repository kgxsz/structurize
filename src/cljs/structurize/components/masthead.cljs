(ns structurize.components.masthead
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

;; TODO - spec everywhere

;; model ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def +image (in [:masthead :avatar-image]))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn masthead-body-left [Φ {:keys [width col-n col-width gutter margin-left]}]
  (let [avatar-url (track Φ l/view-single
                          (in [:auth :me :avatar-url]))]
    [:div.l-cell.l-cell--justify-end {:style {:width (+ width margin-left)}}
     [:div.l-cell.l-cell--justify-center {:style {:width col-width}}
      [:div.c-masthead__avatar
       [image Φ {:+image +image
                 :src avatar-url}]]]]))


(defn masthead-body-center [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
  [:div.l-cell.l-cell--align-center.l-cell--width-100 {:style {:width (+ width margin-left margin-right)
                                                               :padding-left (+ margin-left gutter)
                                                               :padding-right (+ margin-right gutter)}}
   [:span.c-text.c-text--p-size-large
    "The Something Collection"]])

(defn masthead-lip-center [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
  [:div.l-cell.l-cell--align-center.l-cell--height-100 {:style {:width (+ width margin-left margin-right)
                                                                :padding-left (+ margin-left gutter)
                                                                :padding-right (+ margin-right gutter)}}
   [:span.c-text.c-text--p-size-xx-large.c-text--color-white-a
    "Keigo's Superstore"]])

(defn masthead-lip-left [Φ {:keys [width col-n col-width gutter margin-left]}]
  [:div.l-cell.l-cell--justify-end {:style {:width (+ width margin-left)}}])

(defn masthead [Φ]
  (log-debug Φ "render masthead")
  [:div.c-masthead
   [:div.c-masthead__lip
    [triptych Φ {:left {:hidden #{:xs :sm}
                        :c [masthead-lip-left]}
                 :center {:hidden #{}
                          :c [masthead-lip-center]}}]]
   [triptych Φ {:left {:hidden #{:xs :sm}
                       :c [masthead-body-left]}
                :center {:hidden #{}
                         :c [masthead-body-center]}}]])
