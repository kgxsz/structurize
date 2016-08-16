(ns structurize.components.root-component
  (:require [structurize.components.component-utils :as u]
            [structurize.components.tooling-component :refer [tooling]]
            [structurize.system.side-effect-bus :refer [side-effect!]]
            [structurize.system.state :refer [track]]
            [traversy.lens :as l]
            [bidi.bidi :as b]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; components


(defn sign-in-with-github [φ]
  (log/debug "render sign-in-with-github")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! φ :auth/initialise-sign-in-with-github))}
   [:div.l-row.l-row--justify-center
    [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--github]
    "sign in with GitHub"]])


(defn sign-out [Φ]
  (log/debug "render sign-out")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! Φ :auth/sign-out))}
   [:div.l-row.l-row--justify-center
    [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--exit]
    "sign out"]])


(defn with-page-load [φ page]
  (let [app-initialised? (track φ l/view-single
                                  (l/in [:app-status])
                                  (partial = :initialised))
        chsk-status-initialising? (track φ l/view-single
                                           (l/in [:comms :chsk-status])
                                           (partial = :initialising))]

    (log/debug "render with-page-load")

    (if (or (not app-initialised?)
            chsk-status-initialising?)
      [:div.c-page
       [:div.l-col.l-col--justify-center
        [:div.c-hero
         [:div.c-icon.c-icon--coffee-cup.c-icon--h-size-large]
         [:div.c-hero__caption "loading"]]]]
      [page φ])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [Φ]
  [with-page-load Φ
   (fn [Φ]
     (let [me (track Φ l/view-single
                       (l/in [:auth :me]))
           star (track Φ l/view-single
                         (l/in [:playground :star]))
           heart (track Φ l/view-single
                          (l/in [:playground :heart]))
           pong (track Φ l/view-single
                         (l/in [:playground :pong]))]

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
     (log/debug "mount sign-in-with-github-page")
     (side-effect! Φ :auth/mount-sign-in-with-github-page)

     (fn [Φ]
       (let [internal-error (track Φ l/view-single
                                   (l/in [:location :query])
                                   (partial = :error))
             external-error (track Φ l/view-single
                                   (l/in [:auth :sign-in-with-github-status])
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
                                            #(side-effect! Φ :general/change-location
                                                           {:path (b/path-for (:routes config-opts) :home)}))}
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
     (log/debug "render unkown-page")

     [:div.c-page
      [:div.l-col.l-col--justify-center
       [:div.c-hero
        [:div.c-icon.c-icon--poop.c-icon--h-size-xx-large]
        [:div.c-hero__caption "Looks like you're lost!"]]

       [:div.l-col.l-col--align-center
        [:button.c-button {:on-click (u/without-propagation
                                      #(side-effect! Φ :general/change-location
                                                     {:path (b/path-for (:routes config-opts) :home)}))}
         [:div.l-row.l-row--justify-center
          [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--home]
          "go home"]]]]])])


(defn root
  [{:keys [config-opts] :as φ}]

  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])]

    (log/debug "mount root")

    (fn []
      (let [handler (track φ l/view-single
                           (l/in [:location :handler]))]

        (log/debug "render root")

        [:div
         (case handler
           :home [home-page φ]
           :sign-in-with-github [sign-in-with-github-page φ]
           :unknown [unknown-page φ])

         (when tooling-enabled?
           [tooling (assoc φ :context {:tooling? true})])]))))
