(ns structurize.components.root-component
  (:require [structurize.components.component-utils :as u]
            [structurize.components.tooling-component :refer [tooling]]
            [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; components


(defn sign-in-with-github [{:keys [config-opts !db emit-side-effect!] :as Φ}]
  (log/debug "mount sign-in-with-github")

  (let [!reply-received? (r/track #(= :reply-received (get-in @!db [:comms :message :sign-in/init-sign-in-with-github :status])))]

    (fn []
      (let [reply-received? @!reply-received?]

        (log/debug "render sign-in-with-github")

        (when reply-received?
          (emit-side-effect! [:general/redirect-to-github])))

      [:div.button.clickable {:on-click (u/without-propagation
                                         #(emit-side-effect! [:general/init-sign-in-with-github]))}
       [:span.button-icon.icon-github]
       [:span.button-text "sign in with GitHub"]])))


(defn sign-out [{:keys [!db emit-side-effect!] :as Φ}]
  (log/debug "render sign-out")
  [:div.button.clickable {:on-click (u/without-propagation #(emit-side-effect! [:general/sign-out]))}
   [:span.button-icon.icon-exit]
   [:span.button-text "sign out"]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{:keys [config-opts !db emit-side-effect!] :as Φ}]
  (let [!me (r/track #(get-in @!db [:comms :message :general/init :reply :me]))
        !star (r/track #(get-in @!db [:playground :star]))
        !heart (r/track #(get-in @!db [:playground :heart]))
        !ping (r/track #(get-in @!db [:playground :ping]))
        !pong (r/track #(get-in @!db [:comms :message :playground/ping :reply :pong] 0))
        !reply-received? (r/track #(= :reply-received (get-in @!db [:comms :message :playground/ping :status])))]

    (log/debug "mount home-page")

    (fn []
      (let [me @!me
            star @!star
            heart @!heart
            ping @!ping
            pong @!pong
            reply-received? @!reply-received?]

        (log/debug "render home-page")

        (when reply-received?
          (emit-side-effect! [:playground/pong {}]))

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
           [:spam.button-text pong]]]]))))


(defn sign-in-with-github-page [{:keys [!db emit-side-effect!] :as Φ}]
  (let [!query (r/track #(get-in @!db [:location :query]))
        !response-received? (r/track #(= :response-received (get-in @!db [:comms :post "/sign-in/github" :status])))
        !post-failed? (r/track #(= :failed (get-in @!db [:comms :post "/sign-in/github" :status])))]

    (log/debug "mount sign-in-with-github-page")

    (fn []
      (let [{:keys [code error] attempt-id :state} @!query
            response-received? @!response-received?
            post-failed? @!post-failed?]

        (log/debug "render sign-in-with-github-page")

        (cond
          (and code attempt-id) (emit-side-effect! [:general/sign-in-with-github {:code code, :attempt-id attempt-id}])
          response-received? (emit-side-effect! [:general/change-location {:path (b/path-for routes :home)}]))

        (if (or error post-failed?)

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
            [:h1.hero-caption "Signing you in with GitHub"]]])))))


(defn unknown-page [{:keys [emit-side-effect!] :as Φ}]
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
     [:span.button-text "go home"]]]])


(defn loading [φ]
  (log/debug "render loading")
  [:div.loading
   [:span.icon.icon-coffee-cup]
   [:h5.loading-caption "loading"]])


(defn page

  "The page is charged with mounting the relevant page, given the current handler."

  [{:keys [!db] :as Φ}]

  (log/debug "mount page-container")

  (let [!handler (r/track #(get-in @!db [:location :handler]))]

    (fn []
      (let [handler @!handler]

        (log/debug "render page-container")

        (case handler
          :home [home-page Φ]
          :sign-in-with-github [sign-in-with-github-page Φ]
          :unknown [unknown-page Φ])))))


(defn root

  "The root is responsible for mounting the top level components.
   It will send a message to the server to receive the initialising data required by the top
   level components. It will wait until a reply is received before mounting the page. "

  [{:keys [config-opts !db emit-side-effect!] :as Φ}]

  (let [tooling-enabled? (get-in config-opts [:tooling :enabled?])
        !chsk-status-open? (r/track #(= :open (get-in @!db [:comms :chsk-status])))
        !uninitialised? (r/track #(nil? (get-in @!db [:comms :message :general/init :reply])))
        !initialising? (r/track #(= :sent (get-in @!db [:comms :message :general/init :status])))]

    (log/debug "mount root")

    (fn []
      (let [chsk-status-open? @!chsk-status-open?
            uninitialised? @!uninitialised?
            initialising? @!initialising?]

        (log/debug "render root")

        (when (and uninitialised? (not initialising?) chsk-status-open?)
          (emit-side-effect! [:general/general-init]))

        [:div.top-level-container

         (if (or uninitialised? initialising?)
           [loading Φ]
           [page Φ])

         (when tooling-enabled?
           [tooling Φ])]))))
