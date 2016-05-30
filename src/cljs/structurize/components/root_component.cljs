(ns structurize.components.root-component
  (:require [structurize.routes :refer [routes]]
            [structurize.components.component-utils :as u]
            [structurize.components.tooling-component :refer [tooling]]
            [bidi.bidi :as b]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; components


(defn sign-in-with-github [{:keys [config-opts !db emit-side-effect!] :as Φ}]
  (log/debug "render sign-in-with-github")
  [:div.button.clickable {:on-click (u/without-propagation
                                     #(emit-side-effect! [:general/init-sign-in-with-github]))}
   [:span.button-icon.icon-github]
   [:span.button-text "sign in with GitHub"]])


(defn sign-out [{:keys [!db emit-side-effect!] :as Φ}]
  (log/debug "render sign-out")
  [:div.button.clickable {:on-click (u/without-propagation #(emit-side-effect! [:general/sign-out]))}
   [:span.button-icon.icon-exit]
   [:span.button-text "sign out"]])


(defn with-page-status [{:keys [!db emit-side-effect!] :as φ} page]
  (let [!app-initialised? (r/track #(= :initialised (:app-status @!db)))
        !chsk-status-initialising? (r/track #(= :initialising (get-in @!db [:comms :chsk-status])))]

    (log/debug "mount with-page-status")

    (fn []
      (let [app-initialised? @!app-initialised?
            chsk-status-initialising? @!chsk-status-initialising?]

        (log/debug "render with-page-status")

        (if (or (not app-initialised?)
                chsk-status-initialising?)
          [:div.loading
           [:span.icon.icon-coffee-cup]
           [:h5.loading-caption "loading"]]
          [page])))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{:keys [config-opts !db emit-side-effect!] :as Φ}]

  [with-page-status Φ

   (fn []
     (let [!me (r/track #(:me @!db))
           !star (r/track #(get-in @!db [:playground :star]))
           !heart (r/track #(get-in @!db [:playground :heart]))
           !pong (r/track #(get-in @!db [:playground :pong]))]

       (log/debug "mount home-page")

       (fn []
         (let [me @!me
               star @!star
               heart @!heart
               pong @!pong]

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
                                                #(emit-side-effect! [:playground/inc-item {:path [:playground :star]
                                                                                           :item-name "star"}]))}
              [:span.button-icon.icon-star]
              [:span.button-text star]]

             [:div.button.clickable {:on-click (u/without-propagation
                                                #(emit-side-effect! [:playground/inc-item {:path [:playground :heart]
                                                                                           :item-name "heart"}]))}
              [:span.button-icon.icon-heart]
              [:span.button-text heart]]

             [:div.button.clickable {:on-click (u/without-propagation
                                                #(emit-side-effect! [:playground/ping {}]))}
              [:span.button-icon.icon-heart-pulse]
              [:spam.button-text pong]]]]))))])


(defn sign-in-with-github-page [{:keys [!db emit-side-effect!] :as Φ}]
  [with-page-status Φ

   (fn []
     (let [!query (r/track #(get-in @!db [:location :query]))]

       (log/debug "mount sign-in-with-github-page")
       (emit-side-effect! [:general/mount-sign-in-with-github-page])

       (fn []
         (let [{:keys [error]} @!query]

           (log/debug "render sign-in-with-github-page")

           (if error

             [:div.page
              [:div.hero
               [:div.hero-visual
                [:span.icon.icon-github]
                [:span.hero-visual-divider "+"]
                [:span.icon.icon-poop]]
               [:h1.hero-caption "Sign in with GitHub failed"]]

              [:div.options-section
               [:div.button.clickable {:on-click (u/without-propagation
                                                  #(emit-side-effect! [:general/change-location {:path (b/path-for routes :home)}]))}
                [:span.button-icon.icon-home]
                [:span.button-text "go home"]]]]

             [:div.page.sign-in-with-github-page
              [:div.hero
               [:div.hero-visual
                [:span.icon.icon-github]
                [:span.hero-visual-divider "+"]
                [:span.icon.icon-clock]]
               [:h1.hero-caption "Signing you in with GitHub"]]])))))])


(defn unknown-page [{:keys [emit-side-effect!] :as Φ}]
  [with-page-status Φ

   (fn []
     (log/debug "render unkown-page")

     [:div.page
      [:div.hero
       [:div.hero-visual
        [:span.icon.icon-poop]]
       [:h1.hero-caption "Looks like you're lost"]]

      [:div.options-section
       [:div.button.clickable {:on-click (u/without-propagation
                                          #(emit-side-effect! [:general/change-location {:path (b/path-for routes :home)}]))}
        [:span.button-icon.icon-home]
        [:span.button-text "go home"]]]])])


(defn root

  [{:keys [config-opts !db emit-side-effect!] :as Φ}]

  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])
        !handler (r/track #(get-in @!db [:location :handler]))]

    (log/debug "mount root")

    (fn []
      (let [handler @!handler]

        (log/debug "render root")

        [:div.viewport

         (case handler
           :home [home-page Φ]
           :sign-in-with-github [sign-in-with-github-page Φ]
           :unknown [unknown-page Φ])

         (when tooling-enabled?
           [tooling Φ])]))))
