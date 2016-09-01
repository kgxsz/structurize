(ns structurize.components.unknown-page
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.utils :as u]
            [structurize.components.with-page-load :refer [with-page-load]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn unknown-page [{:keys [config-opts] :as Φ}]
  [with-page-load Φ
   (fn [Φ]
     (log-debug Φ "render unkown-page")

     [:div.c-page
      [:div.l-col.l-col--justify-center
       [:div.c-hero
        [:div.c-icon.c-icon--poop.c-icon--h-size-xx-large]
        [:div.c-hero__caption "Looks like you're lost!"]]

       [:div.l-col.l-col--align-center
        [:button.c-button {:on-click (u/without-propagation
                                      #(side-effect! Φ :unknown-page/go-home))}
         [:div.l-row.l-row--justify-center
          [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--home]
          "go home"]]]]])])


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmethod process-side-effect :unknown-page/go-home
  [{:keys [config-opts] :as Φ} id props]
  (change-location! Φ {:path (b/path-for (:routes config-opts) :home)}))
