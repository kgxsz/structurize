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
  [:div.button.clickable {:on-click (u/without-propagation
                                     #(side-effect! [:auth/initialise-sign-in-with-github]))}
   [:span.button-icon.icon-github]
   [:span.button-text "sign in with GitHub"]])


(defn sign-out [{:keys [side-effect!] :as Φ}]
  (log/debug "render sign-out")
  [:div.button.clickable {:on-click (u/without-propagation #(side-effect! [:auth/sign-out]))}
   [:span.button-icon.icon-exit]
   [:span.button-text "sign out"]])


(defn with-page-load [{:keys [track-app side-effect!] :as φ} page]
  (let [app-initialised? (track-app l/view-single (l/in [:app-status]) (partial = :initialised))
        chsk-status-initialising? (track-app l/view-single (l/in [:comms :chsk-status]) (partial = :initialising))]

    (log/debug "render with-page-load")

    (if (or (not app-initialised?)
            chsk-status-initialising?)
      [:div.c-page
         [:div.l-col.l-col--justify-center
          [:div.c-hero
           [:div.c-icon.c-icon--coffee-cup.c-icon--hero]
           [:div.c-hero__caption "Loading"]]]]
      [page])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{:keys [config-opts track-app side-effect!] :as Φ}]
  [with-page-load Φ
   (fn []
     (let [me (track-app l/view-single (l/in [:auth :me]))
           star (track-app l/view-single (l/in [:playground :star]))
           heart (track-app l/view-single (l/in [:playground :heart]))
           pong (track-app l/view-single (l/in [:playground :pong]))]

       (log/debug "render home-page")

       [:div.page

        (if me
            [:div.hero
             [:div.hero-visual
              [:img.large-avatar {:src (:avatar-url me)}]]
             [:h1.hero-caption "Hello @" (:login me)]]

            [:div.hero
             [:div.hero-visual
              [:span.icon.icon-mustache]]
             [:h1.hero-caption "Hello there"]])

        [:div.options-section

           (if me
             [sign-out Φ]
             [sign-in-with-github Φ])

           [:div.button.clickable {:on-click (u/without-propagation
                                              #(side-effect! [:playground/inc-item
                                                              {:path [:playground :star]
                                                               :item-name "star"}]))}
            [:span.button-icon.icon-star]
            [:span.button-text star]]

           [:div.button.clickable {:on-click (u/without-propagation
                                              #(side-effect! [:playground/inc-item
                                                              {:path [:playground :heart]
                                                               :item-name "heart"}]))}
            [:span.button-icon.icon-heart]
            [:span.button-text heart]]

           [:div.button.clickable {:on-click (u/without-propagation
                                              #(side-effect! [:playground/ping {}]))}
            [:span.button-icon.icon-heart-pulse]
            [:spam.button-text pong]]]]))])


(defn sign-in-with-github-page [{:keys [config-opts track-app side-effect!] :as Φ}]
  [with-page-load Φ
   (fn []
     (log/debug "mount sign-in-with-github-page")
     (side-effect! [:auth/mount-sign-in-with-github-page])

     (fn []
       (let [internal-error (track-app l/view-single (l/in [:location :query]) (partial = :error))
             external-error (track-app l/view-single (l/in [:auth :sign-in-with-github-status]) (partial = :failed))]

         (log/debug "render sign-in-with-github-page")

         (if (or internal-error external-error)

           [:div.page
            [:div.hero
             [:div.hero-visual
              [:span.icon.icon-github]
              [:span.hero-visual-divider "+"]
              [:span.icon.icon-poop]]
             [:h1.hero-caption "Sign in with GitHub failed"]]

            [:div.options-section
             [:div.button.clickable {:on-click (u/without-propagation
                                                #(side-effect! [:general/change-location
                                                                {:path (b/path-for (:routes config-opts) :home)}]))}
              [:span.button-icon.icon-home]
              [:span.button-text "go home"]]]]

           [:div.page.sign-in-with-github-page
            [:div.hero
             [:div.hero-visual
              [:span.icon.icon-github]
              [:span.hero-visual-divider "+"]
              [:span.icon.icon-clock]]
             [:h1.hero-caption "Signing you in with GitHub"]]]))))])


(defn unknown-page [{:keys [config-opts side-effect!] :as Φ}]
  [with-page-load Φ
   (fn []
     (log/debug "render unkown-page")

     [:div.c-page
      [:div.l-col.l-col--justify-center
       [:div.c-hero
        [:div.c-icon.c-icon--poop.c-icon--hero]
        [:div.c-hero__caption "Looks like you're lost!"]]

       [:div.l-col.l-col--align-center
        [:button.c-button {:on-click (u/without-propagation
                                      #(side-effect! [:general/change-location
                                                      {:path (b/path-for (:routes config-opts) :home)}]))}
         [:div.l-row.l-row--justify-center
          [:div.l-spacing.l-spacing--margin-right-small.c-icon.c-icon--home]
          "go home"]]]]])])


(defn root
  [{:keys [config-opts track-app side-effect!] :as Φ}]

  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])]

    (log/debug "mount root")

    (fn []
      (let [handler (track-app l/view-single (l/in [:location :handler]))]

        (log/debug "render root")

        [:div
         (case handler
           :home [home-page Φ]
           :sign-in-with-github [sign-in-with-github-page Φ]
           :unknown [unknown-page Φ])

         (when tooling-enabled?
           [tooling Φ])]))))
