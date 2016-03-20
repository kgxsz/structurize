(ns structurize.components.root-component
  (:require [structurize.components.tooling-component :refer [tooling]]
            [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn sign-in-with-github [{{:keys [general]} :config-opts
                            {:keys [!core]} :state
                            {:keys [send! change-location!]} :side-effector}]

  (log/debug "mount/render sign-in-with-github")

  (let [!message-status (r/cursor !core [:message-status :sign-in/init-sign-in-with-github])
        !message-reply (r/cursor !core [:message-reply :sign-in/init-sign-in-with-github])]

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
     [:div.button {:on-click (fn [e] (send! [:sign-in/init-sign-in-with-github {}]) (.stopPropagation e))}
      [:span.button-icon.icon-github]
      [:span.button-text
       (case @!message-status
         :sent "signing in!"
         "sign in with GitHub")]]]))


(defn sign-out [{{:keys [!core]} :state
                 {:keys [post!]} :side-effector}]

  (log/debug "mount/render sign-out-with-github")

  (let [!post-status (r/cursor !core [:post-status "/sign-out"])]

    [:div.sign-out
     [:div.button {:on-click (fn [e] (post! ["/sign-out" {}]) (.stopPropagation e))}
      [:span.button-icon.icon-exit]
      [:span.button-text
       (case @!post-status
         :sent "signing out!"
         "sign out")]]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{{:keys [change-location!]} :side-effector
                  :as Φ}]
  (log/debug "mount/render home-page")
  [:div.home-page
   [:span.icon-mustache]
   [:div.hero "Hello there"]
   [sign-in-with-github Φ]
   [sign-out Φ]])


(defn sign-in-with-github-page [{{:keys [!core !query]} :state
                                 {:keys [post! change-location!]} :side-effector
                                 :as Φ}]
  (log/debug "mount/render sign-in-with-github-page")

  (let [{:keys [code error] attempt-id :state} @!query
        !post-status (r/cursor !core [:post-status "/sign-in/github"])]

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


(defn root [{{:keys [!chsk-status !handler]} :state
             {:keys [send!]} :side-effector
             :as Φ}]

  (log/debug "mount/render root-component")

  (when (= :open @!chsk-status)
    (send! [:users/me]))

  [:div
   [tooling Φ]
   (case @!handler
     :home [home-page Φ]
     :sign-in-with-github [sign-in-with-github-page Φ]
     :init [init-page Φ]
     :unknown [unknown-page Φ])])
