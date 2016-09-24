(ns structurize.components.root
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.tooling :refer [tooling]]
            [structurize.components.home-page :refer [home-page]]
            [structurize.components.store-concept-page :refer [store-concept-page]]
            [structurize.components.unknown-page :refer [unknown-page]]
            [structurize.components.loading-page :refer [loading-page]]
            [structurize.components.sign-in-with-github-page :refer [sign-in-with-github-page]]
            [structurize.lens :refer [in]]
            [cljs.core.match :refer-macros [match]]
            [traversy.lens :as l]
            [bidi.bidi :as b]
            [reagent.core :as r])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - spec everywhere

(defn root [{:keys [config-opts] :as φ}]
  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])]
    (log-debug φ "mount root")
    (fn []
      (let [handler (track φ l/view-single
                           (in [:location :handler]))
            width (track φ l/view-single
                         (in [:viewport :width]))
            loading? (track φ l/view
                            (l/+> (in [:app-status]) (in [:comms :chsk-status]))
                            (fn [[app-status chsk-status]]
                              (match [app-status chsk-status]
                                 [_ :initialising] true
                                 [:uninitialised _] true
                                 [:initialising _] true
                                 :else false)))]

        (log-debug φ "render root")

        [:div.l-cell.l-cell--height-100 {:style {:width width}}
         (match [loading? handler]
           [true _] [loading-page φ]
           [_ :home] [home-page φ]
           [_ :store-concept] [store-concept-page φ]
           [_ :sign-in-with-github] [sign-in-with-github-page φ]
           [_ :unknown] [unknown-page φ])

         (when tooling-enabled?
           [tooling (assoc φ :context {:tooling? true})])]))))
