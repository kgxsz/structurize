(ns structurize.components.root
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.general :as g]
            [structurize.components.tooling :refer [tooling]]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [bidi.bidi :as b]
            [reagent.core :as r])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [structurize.components.macros :refer [log-info log-debug log-error]]))


;; helper components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sign-in-with-github [φ]
  (log-debug φ "render sign-in-with-github")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! φ :auth/initialise-sign-in-with-github))}
   [:div.l-row.l-row--justify-center
    [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--github]
    "sign in with GitHub"]])


(defn sign-out [Φ]
  (log-debug Φ "render sign-out")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! Φ :auth/sign-out))}
   [:div.l-row.l-row--justify-center
    [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--exit]
    "sign out"]])


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


;; top level page components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-page [Φ]
  [with-page-load Φ
   (fn [Φ]
     (let [me (track Φ l/view-single
                       (in [:auth :me]))
           star (track Φ l/view-single
                         (in [:playground :star]))
           heart (track Φ l/view-single
                          (in [:playground :heart]))
           pong (track Φ l/view-single
                         (in [:playground :pong]))]

       (log-debug Φ "render home-page")

       [:div.c-page
        [:div.l-col.l-col--justify-center
         (if me
           [:div.l-col.l-col--align-center.c-hero
            [:div.c-hero__avatar
             [g/image Φ {:+image (in [:home-page :hero-avatar-image])
                         :src (:avatar-url me)}]]
            [:div.c-hero__caption "Hello @" (:login me)]]

           [:div.c-hero
            [:div.c-icon.c-icon--mustache.c-icon--h-size-xx-large]
            [:div.c-hero__caption "Hello there"]])

         [:div.l-col.l-col--align-center
          (if me
            [sign-out Φ]
            [sign-in-with-github Φ])

          [:div.l-spacing.l-spacing--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! Φ :playground/inc-item
                                                        {:path [:playground :star]
                                                           :item-name "star"}))}
            [:div.l-row.l-row--justify-center
             [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--star]
             star]]]

          [:div.l-spacing.l-spacing--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! Φ :playground/inc-item
                                                        {:path [:playground :heart]
                                                         :item-name "heart"}))}
            [:div.l-row.l-row--justify-center
             [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--heart]
             heart]]]

          [:div.l-spacing.l-spacing--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! Φ :playground/ping {}))}
            [:div.l-row.l-row--justify-center
             [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--heart-pulse]
             pong]]]]]]))])


(defn sign-in-with-github-page [{:keys [config-opts] :as Φ}]
  [with-page-load Φ
   (fn [Φ]
     (log-debug Φ "mount sign-in-with-github-page")
     (side-effect! Φ :auth/mount-sign-in-with-github-page)

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
                                            #(side-effect! Φ :general/go-home))}
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


(defn unknown-page [{:keys [config-opts] :as Φ}]
  [with-page-load Φ
   (fn [Φ]
     (log-debug Φ "render unkown-page")

     [:div.c-page
      [:div.l-col.l-col--justify-center
       [:div.c-hero
        [:div.c-icon.c-icon--poop.c-icon--h-size-xx-large]
        [:div.c-hero__caption "Looks like you're lost!"]]

       [:div.l-col.l-col--align-center
        [:button.c-button {:on-click (u/without-propagation
                                      #(side-effect! Φ :general/go-home))}
         [:div.l-row.l-row--justify-center
          [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--home]
          "go home"]]]]])])


;; root component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn root
  [{:keys [config-opts] :as φ}]

  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])]

    (log-debug φ "mount root")

    (fn []
      (let [handler (track φ l/view-single
                           (in [:location :handler]))]

        (log-debug φ "render root")

        [:div
         (case handler
           :home [home-page φ]
           :sign-in-with-github [sign-in-with-github-page φ]
           :unknown [unknown-page φ])

         (when tooling-enabled?
           [tooling (assoc φ :context {:tooling? true})])]))))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :general/go-home
  [{:keys [config-opts] :as Φ} id props]
  (change-location! Φ {:path (b/path-for (:routes config-opts) :home)}))


(defmethod process-side-effect :auth/initialise-sign-in-with-github
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


(defmethod process-side-effect :auth/mount-sign-in-with-github-page
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


(defmethod process-side-effect :auth/sign-out
  [Φ id props]
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


(defmethod process-side-effect :playground/inc-item
  [Φ id {:keys [path item-name] :as props}]
  (let [mutation-id (keyword (str "playground/inc-" item-name))]
    (write! Φ mutation-id
            (fn [x]
              (update-in x path inc)))))


(defmethod process-side-effect :playground/ping
  [Φ id props]
  (let [ping (read Φ l/view-single
                   (in [:playground :ping]))]

    (write! Φ :playground/ping
            (fn [x]
              (update-in x [:playground :ping] inc)))

    (send! Φ :playground/ping
           {:ping (inc ping)}
           {:on-success (fn [[id payload]]
                          (write! Φ :playground/pong
                                  (fn [x]
                                    (assoc-in x [:playground :pong] (:pong payload)))))
            :on-failure (fn [reply] (write! Φ :playground/ping-failed
                                           (fn [x]
                                             (assoc-in x [:playground :ping-status] :failed))))})))
