(ns structurize.components.home-page
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
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn counter [Φ]
  (log-debug Φ "render counter")
  (let [count (track Φ l/view-single
                     (in [:count]))]
    [:a.c-link.c-link--margin-top-large {:on-click (u/without-propagation
                                                    #(side-effect! Φ :home-page/inc-count))}
     [:span.c-icon.c-icon--heart]
     [:span.c-text.c-text--margin-left-x-small count]]))

(defn home-page [Φ]
  (log-debug Φ "render home-page")
  (let [me (track Φ l/view-single
                  (in [:auth :me]))]
    [:div.l-cell.l-cell--fill-parent
     [:div.l-col.l-col--align-center.l-col--padding-top-25
      [:div.l-row
       [:span.c-icon.c-icon--diamond.c-icon--h-size-medium]
       [:span.c-text.c-text--h-size-medium.c-text--margin-left-medium "Structurize"]]

      [:div.l-col.l-col--align-center.l-col--margin-top-xx-large
       [:a.c-link {:on-click (u/without-propagation
                              #(side-effect! Φ :home-page/go-to-component-guide))}
        [:span.c-icon.c-icon--layers]
        [:span.c-text.c-text--margin-left-x-small "Component Guide"]]

       [:a.c-link.c-link--margin-top-large {:on-click (u/without-propagation
                                                       #(side-effect! Φ :home-page/go-to-store-concept))}
        [:span.c-icon.c-icon--crop]
        [:span.c-text.c-text--margin-left-x-small "Design Concepts"]]

       [:a.c-link.c-link--margin-top-large {:on-click (u/without-propagation
                                                        #(side-effect! Φ (if me
                                                                           :home-page/sign-out
                                                                           :home-page/initialise-sign-in-with-github)))}
        [:span.c-icon.c-icon--github]
        [:span.c-text.c-text--margin-left-x-small (if me "Sign out" "Sign in")]]

       [counter Φ]]]]))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :home-page/initialise-sign-in-with-github
  [{:keys [config-opts] :as Φ} id props]
  (send! Φ :auth/initialise-sign-in-with-github
         {}
         {:on-success (fn [[_ {:keys [client-id attempt-id scope redirect-prefix]}]]
                        (let [redirect-uri (str redirect-prefix (b/path-for (:routes config-opts) :sign-in-with-github))]
                          (change-location! Φ {:prefix "https://github.com"
                                               :path "/login/oauth/authorize"
                                               :query {:client_id client-id
                                                       :state attempt-id
                                                       :scope scope
                                                       :redirect_uri redirect-uri}})))
          :on-failure (fn [reply]
                        (write! Φ :auth/sign-in-with-github-failed
                                (fn [x]
                                  (assoc-in x [:auth :sign-in-with-github-failed?] true))))}))


(defmethod process-side-effect :home-page/sign-out
  [{:keys [config-opts] :as Φ} id props]
  (post! Φ "/sign-out"
         {}
         {:on-success (fn [response]
                        (write! Φ :auth/sign-out
                                (fn [x]
                                  (assoc x :auth {}))))
          :on-failure (fn [response]
                        (write! Φ :auth/sign-out-failed
                                (fn [x]
                                  (assoc-in x [:auth :sign-out-status] :failed))))}))


(defmethod process-side-effect :home-page/go-to-store-concept
  [{:keys [config-opts] :as Φ} id props]
  (change-location! Φ {:path (b/path-for (:routes config-opts) :store-concept)}))


(defmethod process-side-effect :home-page/go-to-component-guide
  [{:keys [config-opts] :as Φ} id props]
  (change-location! Φ {:path (b/path-for (:routes config-opts) :component-guide)}))


(defmethod process-side-effect :home-page/inc-count
  [{:keys [config-opts] :as Φ} id props]
  (write! Φ :home-page/inc-count
          (fn [x]
            (l/update x (in [:count]) inc))))
