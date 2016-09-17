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

;; TODO - don't pass anon fns
;; TODO - co-locate CSS
;; TODO - use BEM utility
;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn unknown-page [{:keys [config-opts] :as Φ}]
  [with-page-load Φ
   (fn [Φ]
     (log-debug Φ "render unkown-page")
     [:div.c-page
      [:div.l-col.l-col--align-center.l-col--margin-top-xxx-large
       [:div.c-icon.c-icon--poop.c-icon--h-size-large]
       [:div.l-cell.l-cell--margin-top-medium
        [:span.c-text.c-text--p-size-xx-large "Looks like you're lost!"]]
       [:div.l-cell.l-cell--margin-top-small
        [:span.c-text.c-text--p-size-small "This page doesn't exist, or it has been removed"]]]])])
