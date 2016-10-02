(ns structurize.components.components-page
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

;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn components-page [φ]
  (log-debug φ "render components-page")
  [:div.l-col.l-col--align-center.l-col--padding-top-25
   [:div.c-icon.c-icon--construction.c-icon--h-size-medium]
   [:div.c-text.c-text--margin-top-medium "This page is under construction."]])

