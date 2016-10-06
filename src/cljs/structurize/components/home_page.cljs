(ns structurize.components.home-page
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.image :refer [image]]
            [structurize.components.triptych :refer [triptych]]
            [structurize.components.header :refer [header]]
            [structurize.components.hero :refer [hero]]
            [structurize.components.masthead :refer [masthead]]
            [structurize.components.pod :refer [pod]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [structurize.styles.vars :refer [vars]]
            [garden.color :as c]
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


(defn home-page-left [Φ {:keys [width col-n col-width gutter margin-left]}]
  [:div.l-col.l-col--align-start {:style {:width width
                                          :padding-left gutter
                                          :margin-left margin-left}}
   [:div {:style {:width col-width}}
    (doall
     (for [i (range 2)]
       [:div {:key i
              :style {:margin-top gutter}}
        [pod Φ]]))]])


(defn home-page-center [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
  [:div.l-row.l-row--justify-space-between {:style {:width width
                                                    :padding-left gutter
                                                    :padding-right gutter
                                                    :margin-left margin-left
                                                    :margin-right margin-right}}
   (doall
    (for [i (range col-n)]
      [:div {:key i
             :style {:width col-width}}
       (doall
        (for [j (range 6)]
          [:div {:key j
                 :style {:margin-top gutter}}
           [pod Φ]]))]))])


(defn home-page-right [Φ {:keys [width col-n col-width gutter margin-right]}]
  [:div.l-col.l-col--align-end {:style {:width width
                                        :padding-right gutter
                                        :margin-right margin-right}}
   [:div {:style {:width col-width}}
    (doall
     (for [i (range 1)]
       [:div {:key i
              :style {:margin-top gutter}}
        [pod Φ]]))]])


(defn home-page [Φ]
  (log-debug Φ "render home-page")
  [:div.l-cell.l-cell--margin-bottom-medium
   [header Φ]
   [hero Φ]
   [masthead Φ]
   [triptych Φ {:left {:hidden #{:xs :sm}
                       :c [home-page-left]}
                :center {:hidden #{}
                         :c [home-page-center]}
                :right {:hidden #{:xs :sm :md}
                        :c [home-page-right]}}]])
