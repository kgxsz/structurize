(ns structurize.components.with-page-load
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn with-page-load [φ page]
  (let [app-initialised? (track φ l/view-single
                                (in [:app-status])
                                (partial = :initialised))
        chsk-status-initialising? (track φ l/view-single
                                         (in [:comms :chsk-status])
                                         (partial = :initialising))]

    (log-debug φ "render with-page-load")

    (if (or (not app-initialised?)
            chsk-status-initialising?)
      [:div.c-page
       [:div.l-col.l-col--justify-center
        [:div.c-hero
         [:div.c-icon.c-icon--coffee-cup.c-icon--h-size-large]
         [:div.c-hero__caption "loading"]]]]
      [page φ])))