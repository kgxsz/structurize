(ns structurize.components.root-component
  (:require [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [camel-snake-kebab.core :as csk]
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

    [:div
     [:button
      {:on-click (fn [] (send! [:sign-in/init-sign-in-with-github {}]))}
      (case @!message-status
        :sent "signing in!"
        "sign in with GitHub")]]))


(defn sign-out [{{:keys [!core]} :state
                 {:keys [post!]} :side-effector}]

  (log/debug "mount/render sign-out-with-github")

  (let [!post-status (r/cursor !core [:post-status "/sign-out"])]

    [:div
     [:button
      {:on-click (fn [] (post! ["/sign-out" {}]))}
      (case @!post-status
        :sent "signing out!"
        "sign out")]]))


(defn event-state-watch [{:keys [state]}]
  (log/debug "mount/render event-state-watch")
  [:span (str @(:!core state))])




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{{:keys [change-location!]} :side-effector
                  :as Φ}]
  (log/debug "mount/render home-page")
  [:div
   [:h1 "Front end ready!"]
   [sign-in-with-github Φ]
   [sign-out Φ]
   [event-state-watch Φ]])


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

       [event-state-watch Φ]
       [:button {:on-click  #(change-location! {:path (b/path-for routes :home)})}
        "home"]]

      [:div
       [:h1 "We're signing you in with github!"]
       [event-state-watch Φ]
       [:button {:on-click  #(change-location! {:path (b/path-for routes :home)})}
        "home"]])))


(defn unknown-page [{{:keys [change-location!]} :side-effector
                     :as Φ}]
  (log/debug "mount/render unkown-page")
  [:div
   [:h1 "What?! Where the hell am I?"]
   [event-state-watch Φ]
   [:button {:on-click  #(change-location! {:path (b/path-for routes :home)})}
    "Go home!"]])


(defn init-page []
  (log/debug "mount/render init-page")
  [:div
   [:h1 "Loading your stuff!"]])


(defn root [{{:keys [!chsk-status !handler]} :state
             {:keys [send!]} :side-effector
             :as Φ}]

  (log/debug "mount/render root-component")

  (when (= :open @!chsk-status)
    (send! [:users/me]))

  (case @!handler
    :home [home-page Φ]
    :sign-in-with-github [sign-in-with-github-page Φ]
    :init [init-page Φ]
    :unknown [unknown-page Φ]))
