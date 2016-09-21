(ns structurize.components.loading-page
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

;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn loading-page [φ]
  (log-debug φ "render loading-page")
  [:div.c-page
   [:div.l-col.l-col--align-center.l-col--margin-top-xxx-large
    [:div.c-icon.c-icon--coffee-cup.c-icon--h-size-large]
    [:div.l-cell.l-cell--margin-top-medium
     [:span.c-text.c-text--p-size-xx-large "Loading"]]]])
