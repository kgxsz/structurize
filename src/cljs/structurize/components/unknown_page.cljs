(ns structurize.components.unknown-page
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn unknown-page [φ]
  (log-debug φ "render unkown-page")
  [:div.l-cell.l-cell--fill-parent.l-cell--justify-center.l-cell--align-center
   [:div.l-col.l-col--align-center
    [:div.c-icon.c-icon--poop.c-icon--h-size-medium.c-icon--color-white-d]]])
