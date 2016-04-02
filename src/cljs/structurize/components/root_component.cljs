(ns structurize.components.root-component
  (:require [structurize.components.component-utils :as u]
            [structurize.components.tooling-component :refer [tooling]]
            [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn sign-in-with-github [{{:keys [general]} :config-opts
                            {:keys [!db]} :state
                            {:keys [send! change-location!]} :side-effector}]

  (log/debug "mount/render sign-in-with-github")

  (let [!message-status (r/cursor !db [:comms :message :sign-in/init-sign-in-with-github :status])
        !message-reply (r/cursor !db [:comms :message :sign-in/init-sign-in-with-github :reply])]

    (when (= :reply-received @!message-status)
      (let [{:keys [client-id attempt-id scope]} @!message-reply
            redirect-uri (str (:host general) (b/path-for routes :sign-in-with-github))]
        (change-location! {:prefix "https://github.com"
                           :path "/login/oauth/authorize"
                           :query {:client_id client-id
                                   :state attempt-id
                                   :scope scope
                                   :redirect_uri redirect-uri}})))

    [:div.sign-in-with-github
     [:div.button.clickable {:on-click (fn [e] (send! [:sign-in/init-sign-in-with-github {}]) (.stopPropagation e))}
      [:span.button-icon.icon-github]
      [:span.button-text "sign in with GitHub"]]]))


(defn sign-out [{{:keys [!db]} :state
                 {:keys [post!]} :side-effector}]

  (log/debug "mount/render sign-out-with-github")

  (let [!post-status (r/cursor !db [:comms :post "/sign-out" :status])]

    [:div.sign-out
     [:div.button.clickable {:on-click (fn [e] (post! ["/sign-out" {}]) (.stopPropagation e))}
      [:span.button-icon.icon-exit]
      [:span.button-text "sign out"]]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{{:keys [!db]} :state
                  {:keys [change-location! emit-event!]} :side-effector
                  :as Φ}]
  (log/debug "mount/render home-page")

  [:div.home-page

   (if-let [me (get-in @!db [:comms :message :users/me :reply])]

     [:div.me-context
      [:img.avatar {:src (:avatar-url me)}]
      [:div.hero "Hello @" (:login me)]
      [sign-out Φ]]

     [:div.me-context
      [:span.icon-mustache]
      [:div.hero "Hello there"]
      [sign-in-with-github Φ]])

   [:div.playground
    [:div.button.clickable {:on-click (u/without-propagation #(emit-event! [:playground/inc-star {:Δ (fn [c] (update-in c [:playground :star] inc))}]))}
     [:span.button-icon.icon-star]
     [:span.button-text (get-in @!db [:playground :star])]]
    [:div.button.clickable {:on-click (u/without-propagation #(emit-event! [:playground/inc-heart {:Δ (fn [c] (update-in c [:playground :heart] inc))}]))}
     [:span.button-icon.icon-heart]
     [:span.button-text (get-in @!db [:playground :heart])]]]])


(defn sign-in-with-github-page [{{:keys [!db !query]} :state
                                 {:keys [post! change-location!]} :side-effector
                                 :as Φ}]
  (log/debug "mount/render sign-in-with-github-page")

  (let [{:keys [code error] attempt-id :state} @!query
        !post-status (r/cursor !db [:comms :post "/sign-in/github" :status])]

    (cond
      (and code attempt-id) (do (post! ["/sign-in/github" {:code code, :attempt-id attempt-id}])
                                (change-location! {:query {} :replace? true}))
      (= :response-received @!post-status) (change-location! {:path (b/path-for routes :home)}))

    (if (or error (= :failed @!post-status))

      [:div
       [:h1 "Sign in with GitHub failed "]
       [:h3 "Couldn't complete the sign in process with Github."]

       [:button {:on-click #(change-location! {:path (b/path-for routes :home)})}
        "home"]]

      [:div
       [:h1 "We're signing you in with github!"]
       [:button {:on-click  #(change-location! {:path (b/path-for routes :home)})}
        "home"]])))


(defn unknown-page [{{:keys [change-location!]} :side-effector
                     :as Φ}]
  (log/debug "mount/render unkown-page")
  [:div
   [:h1 "What?! Where the hell am I?"]
   [:button {:on-click #(change-location! {:path (b/path-for routes :home)})}
    "Go home!"]])


(defn init-page []
  (log/debug "mount/render init-page")
  [:div.init-page
   [:span.icon.icon-coffee-cup]
   [:div "loading"]])


(defn root [{{:keys [renderer]} :config-opts
             {:keys [!chsk-status !handler]} :state
             {:keys [send!]} :side-effector
             :as Φ}]

  (log/debug "mount/render root-component")

  (when (= :open @!chsk-status)
    (send! [:users/me]))

  [:div
   (when (:tooling-enabled? renderer)
     [tooling Φ])
   (case @!handler
     :home [home-page Φ]
     :sign-in-with-github [sign-in-with-github-page Φ]
     :init [init-page Φ]
     :unknown [unknown-page Φ])])
