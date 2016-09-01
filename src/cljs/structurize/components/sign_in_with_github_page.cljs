(ns structurize.components.sign-in-with-github-page
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

(defn sign-in-with-github-page [{:keys [config-opts] :as Φ}]
  [with-page-load Φ
   (fn [Φ]
     (log-debug Φ "mount sign-in-with-github-page")
     (side-effect! Φ :sign-in-with-github-page/did-mount)

     (fn [Φ]
       (let [internal-error (track Φ l/view-single
                                   (in [:location :query])
                                   (partial = :error))
             external-error (track Φ l/view-single
                                   (in [:auth :sign-in-with-github-status])
                                   (partial = :failed))]

         (log-debug Φ "render sign-in-with-github-page")

         [:div.c-page
          (if (or internal-error external-error)

            [:div.l-col.l-col--justify-center
             [:div.c-hero
              [:div.l-row.l-row--justify-center
               [:div.c-icon.c-icon--github.c-icon--h-size-xx-large]
               [:div.c-hero__inter-icon "+"]
               [:div.c-icon.c-icon--poop.c-icon--h-size-xx-large]]
              [:div.c-hero__caption "Sign in with GitHub failed!"]]

             [:div.l-col.l-col--align-center
              [:button.c-button {:on-click (u/without-propagation
                                            #(side-effect! Φ :sign-in-with-gitub-page/go-home))}
               [:div.l-row.l-row--justify-center
                [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--home]
                "go home"]]]]

            [:div.l-col.l-col--justify-center
             [:div.c-hero
              [:div.l-row.l-row--justify-center
               [:div.c-icon.c-icon--github.c-icon--h-size-xx-large]
               [:div.c-hero__inter-icon "+"]
               [:div.c-icon.c-icon--clock.c-icon--h-size-xx-large]]
              [:div.c-hero__caption "Signing you in with GitHub"]]])])))])



;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :sign-in-with-github-page/go-home
  [{:keys [config-opts] :as Φ} id props]
  (change-location! Φ {:path (b/path-for (:routes config-opts) :home)}))


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
