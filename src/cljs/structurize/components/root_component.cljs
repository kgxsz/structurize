(ns structurize.components.root-component
  (:require [structurize.components.component-utils :as u]
            [structurize.components.tooling-component :refer [tooling]]
            [traversy.lens :as l]
            [bidi.bidi :as b]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; components


(defn sign-in-with-github [{:keys [side-effect!] :as Φ}]
  (log/debug "render sign-in-with-github")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! [:auth/initialise-sign-in-with-github]))}
   [:div.l-row.l-row--justify-center
    [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--github]
    "sign in with GitHub"]])


(defn sign-out [{:keys [side-effect!] :as Φ}]
  (log/debug "render sign-out")
  [:button.c-button {:on-click (u/without-propagation #(side-effect! [:auth/sign-out]))}
   [:div.l-row.l-row--justify-center
    [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--exit]
    "sign out"]])


(defn with-page-load [{:keys [track +app] :as φ} page]
  (let [app-initialised? (track l/view-single
                                (l/*> +app (l/in [:app-status]))
                                (partial = :initialised))
        chsk-status-initialising? (track l/view-single
                                         (l/*> +app (l/in [:comms :chsk-status]))
                                         (partial = :initialising))]

    (log/debug "render with-page-load")

    (if (or (not app-initialised?)
            chsk-status-initialising?)
      [:div.c-page
       [:div.l-col.l-col--justify-center
        [:div.c-hero
         [:div.c-icon.c-icon--coffee-cup.c-icon--h-size-large]
         [:div.c-hero__caption "loading"]]]]
      [page])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{:keys [config-opts side-effect! track +app] :as Φ}]
  [with-page-load Φ
   (fn []
     (let [me (track l/view-single
                     (l/*> +app (l/in [:auth :me])))
           star (track l/view-single
                       (l/*> +app (l/in [:playground :star])))
           heart (track l/view-single
                        (l/*> +app (l/in [:playground :heart])))
           pong (track l/view-single
                       (l/*> +app (l/in [:playground :pong])))]

       (log/debug "render home-page")

       [:div.c-page
        [:div.l-col.l-col--justify-center
         (if me
           [:div.c-hero
            [:img.c-hero__avatar {:src (:avatar-url me)}]
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
                                         #(side-effect! [:playground/inc-item
                                                         {:path [:playground :star]
                                                          :item-name "star"}]))}
            [:div.l-row.l-row--justify-center
             [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--star]
             star]]]

          [:div.l-spacing.l-spacing--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! [:playground/inc-item
                                                         {:path [:playground :heart]
                                                          :item-name "heart"}]))}
            [:div.l-row.l-row--justify-center
             [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--heart]
             heart]]]

          [:div.l-spacing.l-spacing--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! [:playground/ping {}]))}
            [:div.l-row.l-row--justify-center
             [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--heart-pulse]
             pong]]]]]]))])


(defn sign-in-with-github-page [{:keys [config-opts side-effect! track +app] :as Φ}]
  [with-page-load Φ
   (fn []
     (log/debug "mount sign-in-with-github-page")
     (side-effect! [:auth/mount-sign-in-with-github-page])

     (fn []
       (let [internal-error (track l/view-single
                                   (l/*> +app (l/in [:location :query]))
                                   (partial = :error))
             external-error (track l/view-single
                                   (l/*> +app (l/in [:auth :sign-in-with-github-status]))
                                   (partial = :failed))]

         (log/debug "render sign-in-with-github-page")

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
                                            #(side-effect! [:general/change-location
                                                            {:path (b/path-for (:routes config-opts) :home)}]))}
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


(defn unknown-page [{:keys [config-opts side-effect!] :as Φ}]
  [with-page-load Φ
   (fn []
     (log/debug "render unkown-page")

     [:div.c-page
      [:div.l-col.l-col--justify-center
       [:div.c-hero
        [:div.c-icon.c-icon--poop.c-icon--h-size-xx-large]
        [:div.c-hero__caption "Looks like you're lost!"]]

       [:div.l-col.l-col--align-center
        [:button.c-button {:on-click (u/without-propagation
                                      #(side-effect! [:general/change-location
                                                      {:path (b/path-for (:routes config-opts) :home)}]))}
         [:div.l-row.l-row--justify-center
          [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--home]
          "go home"]]]]])])


(defn root
  [{:keys [config-opts track +app] :as Φ}]

  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])]

    (log/debug "mount root")

    (fn []
      (let [handler (track l/view-single
                           (l/*> +app (l/in [:location :handler])))]

        (log/debug "render root")

        [:div
         (case handler
           :home [home-page Φ]
           :sign-in-with-github [sign-in-with-github-page Φ]
           :unknown [unknown-page Φ])

         (when tooling-enabled?
           [tooling Φ])]))))
