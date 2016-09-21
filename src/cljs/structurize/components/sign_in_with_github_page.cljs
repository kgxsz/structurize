(ns structurize.components.sign-in-with-github-page
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

;; TODO - don't pass anon fns
;; TODO - co-locate CSS
;; TODO - use BEM utility
;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sign-in-with-github-page [φ ]
  (log-debug φ "mount sign-in-with-github-page")
  (r/create-class
   {:component-did-mount #(side-effect! φ :sign-in-with-github-page/did-mount)
    :reagent-render (fn [φ]
                      (let [internal-error (track φ l/view-single
                                                  (in [:location :query])
                                                  (partial = :error))
                            external-error (track φ l/view-single
                                                  (in [:auth :sign-in-with-github-status])
                                                  (partial = :failed))]

                        (log-debug φ "render sign-in-with-github-page")

                        [:div.c-page
                         (if (or internal-error external-error)
                           [:div.l-col.l-col--align-center.l-col--margin-top-xxx-large
                            [:div.l-row.l-row--justify-center.l-row--align-center
                             [:div.c-icon.c-icon--github.c-icon--h-size-large]
                             [:div.l-cell.l-cell--margin-left-medium.l-cell--margin-right-medium
                              [:span.c-text.c-text--h-size-large "+"]]
                             [:div.c-icon.c-icon--poop.c-icon--h-size-large]]
                            [:div.l-cell.l-cell--margin-top-medium
                             [:span.c-text.c-text--p-size-xx-large "Something went wrong!"]]
                            [:div.l-cell.l-cell--margin-top-small
                             [:span.c-text.c-text--p-size-small "We couldn't sign you in with GitHub"]]]

                           [:div.l-col.l-col--align-center.l-col--margin-top-xxx-large
                            [:div.l-row.l-row--justify-center.l-row--align-center
                             [:div.c-icon.c-icon--github.c-icon--h-size-large]
                             [:div.l-cell.l-cell--margin-left-medium.l-cell--margin-right-medium
                              [:span.c-text.c-text--h-size-large "+"]]
                             [:div.c-icon.c-icon--clock.c-icon--h-size-large]]
                            [:div.l-cell.l-cell--margin-top-medium
                             [:span.c-text.c-text--p-size-xx-large "Signing you in with GitHub"]]])]))}))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :sign-in-with-github-page/did-mount
  [{:keys [config-opts] :as Φ} id props]
  (let [{:keys [code] attempt-id :state} (read Φ l/view-single
                                               (in [:location :query]))]
    (change-location! Φ {:query {} :replace? true})
    (when (and code attempt-id)
      (post! Φ "/sign-in/github"
             {:code code :attempt-id attempt-id}
             {:on-success (fn [response]
                            (change-location! Φ {:path (b/path-for (:routes config-opts) :home)}))
              :on-failure (fn [response]
                            (write! Φ :auth/sign-in-with-github-failed
                                    (fn [x]
                                      (assoc-in x [:auth :sign-in-with-github-failed?] true))))}))))
