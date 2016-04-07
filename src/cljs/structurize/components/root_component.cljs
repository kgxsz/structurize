(ns structurize.components.root-component
  (:require [structurize.components.component-utils :as u]
            [structurize.components.tooling-component :refer [tooling]]
            [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; components


(defn sign-in-with-github [{:keys [config-opts state side-effector] :as Φ}]
  (let [{:keys [!db]} state
        {:keys [send! change-location!]} side-effector
        host (get-in config-opts [:host])]

    (log/debug "mount/render sign-in-with-github")

    (let [!message-status (r/cursor !db [:comms :message :sign-in/init-sign-in-with-github :status])
          !message-reply (r/cursor !db [:comms :message :sign-in/init-sign-in-with-github :reply])]

      (when (= :reply-received @!message-status)
        (let [{:keys [client-id attempt-id scope]} @!message-reply
              redirect-uri (str host (b/path-for routes :sign-in-with-github))]
          (change-location! {:prefix "https://github.com"
                             :path "/login/oauth/authorize"
                             :query {:client_id client-id
                                     :state attempt-id
                                     :scope scope
                                     :redirect_uri redirect-uri}})))

      [:div.button.clickable {:on-click (u/without-propagation #(send! [:sign-in/init-sign-in-with-github {}]))}
       [:span.button-icon.icon-github]
       [:span.button-text "sign in with GitHub"]])))


(defn sign-out [{:keys [state side-effector] :as Φ}]
  (let [{:keys [!db]} state
        {:keys [post!]} side-effector
        !post-status (r/cursor !db [:comms :post "/sign-out" :status])]

    (log/debug "mount/render sign-out")

    [:div.button.clickable {:on-click (u/without-propagation #(post! ["/sign-out" {}]))}
     [:span.button-icon.icon-exit]
     [:span.button-text "sign out"]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{:keys [config-opts state side-effector] :as Φ}]
  (let [{:keys [!db]} state
        {:keys [emit-mutation!]} side-effector
        !me (r/cursor !db [:comms :message :general/init :reply :me])
        !star (r/cursor !db [:playground :star])
        !heart (r/cursor !db [:playground :heart])]

    (log/debug "mount home-page")

    (fn []
      (let [me @!me
            star @!star
            heart @!heart]

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
          [:div.button.clickable {:on-click (u/without-propagation #(emit-mutation! [:playground/inc-star {:cursor !star :Δ inc}]))}
           [:span.button-icon.icon-star]
           [:span.button-text star]]
          [:div.button.clickable {:on-click (u/without-propagation #(emit-mutation! [:playground/inc-heart {:cursor !heart :Δ inc}]))}
           [:span.button-icon.icon-heart]
           [:span.button-text heart]]]]))))


(defn sign-in-with-github-page [{:keys [state side-effector] :as Φ}]
  (let [{:keys [!db !query]} state
        {:keys [post! change-location!]} side-effector
        {:keys [code error] attempt-id :state} @!query
        !post-status (r/cursor !db [:comms :post "/sign-in/github" :status])]

    (log/debug "mount/render sign-in-with-github-page")

    (cond
      (and code attempt-id) (do (change-location! {:query {} :replace? true})
                                (post! ["/sign-in/github" {:code code, :attempt-id attempt-id}]))
      (= :response-received @!post-status) (change-location! {:path (b/path-for routes :home)}))

    (if (or error (= :failed @!post-status))

      [:div.page
       [:div.hero
        [:div.hero-visual
         [:span.icon.icon-github]
         [:span.hero-visual-divider "+"]
         [:span.icon.icon-poop]]
        [:h1.hero-caption "Sign in with GitHub failed"]]

       [:div.options-section
        [:div.button.clickable {:on-click (u/without-propagation #(change-location! {:path (b/path-for routes :home)}))}
         [:span.button-icon.icon-home]
         [:span.button-text "go home"]]]]

      [:div.page.sign-in-with-github-page
       [:div.hero
        [:div.hero-visual
         [:span.icon.icon-github]
         [:span.hero-visual-divider "+"]
         [:span.icon.icon-clock]]
        [:h1.hero-caption "Signing you in with GitHub"]]])))


(defn unknown-page [{:keys [side-effector] :as Φ}]
  (let [{:keys [change-location!]} side-effector]

    (log/debug "mount/render unkown-page")

    [:div.page
     [:div.hero
      [:div.hero-visual
       [:span.icon.icon-poop]]
      [:h1.hero-caption "Looks like you're lost"]]

     [:div.options-section
      [:div.button.clickable {:on-click (u/without-propagation #(change-location! {:path (b/path-for routes :home)}))}
       [:span.button-icon.icon-home]
       [:span.button-text "go home"]]]]))


(defn loading [φ]
  (log/debug "mount/render loading")
  [:div.loading
   [:span.icon.icon-coffee-cup]
   [:h5.loading-caption "loading"]])


(defn page

  "The page is charged with mounting the relevant page, given the current handler."

  [{:keys [state] :as Φ}]

  (log/debug "mount/render page-container")

  (let [{:keys [!handler]} state
        handler @!handler]

    (case handler
      :home [home-page Φ]
      :sign-in-with-github [sign-in-with-github-page Φ]
      :unknown [unknown-page Φ])))


(defn root

  "The root is responsible for mounting the top level components.
   It will send a message to the server to receive the initialising data required by the top
   level components. It will wait until a reply is received before mounting the page. "

  [{:keys [config-opts state side-effector] :as Φ}]

  (let [{:keys [!db !chsk-status]} state
        {:keys [send!]} side-effector
        !message-reply (r/cursor !db [:comms :message :general/init :reply])]

    (log/debug "mount root")

    (fn []
      (let [chsk-status-open? (= :open @!chsk-status)
            initialising? (nil? @!message-reply)
            tooling-enabled? (get-in config-opts [:tooling :enabled?])]

        (log/debug "render root")

        (when (and chsk-status-open? initialising?)
          (send! [:general/init]))

        [:div.top-level-container

         (if initialising?
           [loading Φ]
           [page Φ])

         (when tooling-enabled?
           [tooling Φ])]))))
