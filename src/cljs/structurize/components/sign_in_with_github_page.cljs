(ns structurize.components.sign-in-with-github-page
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [cljs.core.match :refer-macros [match]]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sign-in-with-github-page [φ]
  (log-debug φ "mount sign-in-with-github-page")
  (r/create-class
   {:component-did-mount #(side-effect! φ :sign-in-with-github-page/did-mount)
    :reagent-render (fn [φ]
                      (let [error? (track φ l/view
                                          (l/+> (in [:location :query]) (in [:auth :sign-in-with-github-status]))
                                          (fn [[query sign-in-with-github-status]]
                                            (match [query sign-in-with-github-status]
                                                   [_ :failed] true
                                                   [:error _] true
                                                   :else false)))]

                        (log-debug φ "render sign-in-with-github-page")

                        [:div.l-cell.l-cell--justify-center.l-cell--padding-top-25
                         (if error?
                           [:div.l-col.l-col--align-center
                            [:div.c-icon.c-icon--poop.c-icon--h-size-medium]
                            [:div.c-text.c-text--margin-top-medium "We couldn't sign you in with GitHub."]]
                           [:div.c-icon.c-icon--clock.c-icon--h-size-medium])]))}))


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
