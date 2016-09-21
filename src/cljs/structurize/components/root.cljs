(ns structurize.components.root
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.tooling :refer [tooling]]
            [structurize.components.with-page-load :refer [with-page-load]]
            [structurize.components.home-page :refer [home-page]]
            [structurize.components.unknown-page :refer [unknown-page]]
            [structurize.components.sign-in-with-github-page :refer [sign-in-with-github-page]]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [bidi.bidi :as b]
            [reagent.core :as r])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - don't pass anon fns
;; TODO - co-locate CSS
;; TODO - use BEM utility
;; TODO - spec everywhere

(defn root
  [{:keys [config-opts] :as φ}]

  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])]

    (log-debug φ "mount root")

    (fn []
      (let [handler (track φ l/view-single
                           (in [:location :handler]))
            width (track φ l/view-single
                         (in [:viewport :width]))]

        (log-debug φ "render root")

        [:div {:style {:width width}}
         (case handler
           :home [with-page-load φ {:content [home-page]}]
           :sign-in-with-github [with-page-load φ {:content [sign-in-with-github-page]}]
           :unknown [with-page-load φ {:content [unknown-page]}])

         (when tooling-enabled?
           [tooling (assoc φ :context {:tooling? true})])]))))
