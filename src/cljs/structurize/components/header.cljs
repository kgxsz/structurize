(ns structurize.components.header
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
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


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header [Φ]
  (log-debug Φ "render masthead")
  [:div.c-header
   [triptych Φ {:left {:hidden #{:xs :sm :md}
                       :c (fn [Φ {:keys [width col-n col-width gutter margin-left]}]
                            [:div {:style {:width width
                                           :margin-left margin-left
                                           :padding-left gutter
                                           :padding-top 6
                                           :padding-bottom 6}}
                             [:div.l-cell.l-cell--fill-parent {:style {:background-color "#F9F9F9"}}]])}
                :center {:c (fn [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
                              [:div {:style {:width width
                                             :margin-left margin-left
                                             :margin-right margin-right
                                             :padding-left gutter
                                             :padding-right gutter
                                             :padding-top 6
                                             :padding-bottom 6}}
                               [:div.l-cell.l-cell--fill-parent {:style {:background-color "#EEE"}}]])}
                :right {:hidden #{:xs :sm :md}
                        :c (fn [Φ {:keys [width col-n col-width gutter margin-right]}]
                             [:div {:style {:width width
                                            :margin-right margin-right
                                            :padding-right gutter
                                            :padding-top 6
                                            :padding-bottom 6}}
                              [:div.l-cell.l-cell--fill-parent {:style {:background-color "#F9F9F9"}}]])}}]])

