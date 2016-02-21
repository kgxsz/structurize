(ns structurize.components.root-component
  (:require [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [camel-snake-kebab.core :as csk]
            [reagent.core :as r]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn click-counter-a [{{:keys [!click-count-a]} :state
                        {:keys [emit-event!]} :side-effector}]

  (log/debug "mount/render click-counter-a")
  [:button
   {:on-click #(emit-event! [:inc-click-count/a {:cursor :!click-count-a :Δ inc}])}
   (str "clicks: " @!click-count-a)])


(defn click-counter-b [{{:keys [!click-count-b]} :state
                        {:keys [emit-event!]} :side-effector}]

  (log/debug "mount/render click-counter-b")
  [:button
   {:on-click #(emit-event! [:inc-click-count/b {:cursor :!click-count-b :Δ inc}])}
   (str "clicks: " @!click-count-b)])


(defn login-with-github [{{:keys [general]} :config-opts
                          {:keys [!core]} :state
                          {:keys [send! change-history!]} :side-effector}]

  (log/debug "mount/render login-with-github")

  (let [!message-status (r/cursor !core [:message-status :auth/init-auth-with-github])
        !message-reply (r/cursor !core [:message-reply :auth/init-auth-with-github])]

    (when (= :received @!message-status)
      (let [{:keys [client-id attempt-id scope]} @!message-reply
            redirect-uri (str (:host general) (b/path-for routes :auth-with-github))]
        (change-history! {:prefix "https://github.com"
                          :path "/login/oauth/authorize"
                          :query {:client_id client-id
                                  :state attempt-id
                                  :scope scope
                                  :redirect_uri redirect-uri}})))

    [:div
     [:button
      {:on-click (fn [] (send! {:message [:auth/init-auth-with-github {}]}))}
      (case @!message-status
        :sent "logging in!"
        :received "logging in!"
        "log in with GitHub")]]))


(defn event-state-watch [{:keys [state]}]
  (log/debug "mount/render event-state-watch")
  [:span (str @(:!core state))])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level pages


(defn home-page [{{:keys [change-history!]} :side-effector
                  :as Φ}]
  (log/debug "mount/render home-page")
  [:div
   [:h1 "Front end ready!"]
   [click-counter-a Φ]
   [click-counter-b Φ]
   [login-with-github Φ]
   [event-state-watch Φ]])


(defn auth-with-github-page [{{:keys [!core !query]} :state
                              {:keys [auth! change-history!]} :side-effector
                              :as Φ}]
  (log/debug "mount/render auth-with-github-page")

  (let [{:keys [code error] attempt-id :state} @!query
        !auth-request-status (r/cursor !core [:auth-request-status :github])]

    (cond
      (and code attempt-id) (do (auth! code attempt-id)
                                (change-history! {:query {} :replace? true}))
      (= :succeeded @!auth-request-status) (change-history! {:path (b/path-for routes :home)}))

    (if (or error (= :failed @!auth-request-status))

      [:div
       [:h1 "Login with GitHub failed "]
       [:h3 "Couldn't complete the login process with Github."]

       [event-state-watch Φ]
       [:button {:on-click  #(change-history! {:path (b/path-for routes :home)})}
        "home"]]

      [:div
       [:h1 "We're authorizing you with github!"]
       [event-state-watch Φ]
       [:button {:on-click  #(change-history! {:path (b/path-for routes :home)})}
        "home"]])))


(defn unknown-page [{{:keys [change-history!]} :side-effector
                     :as Φ}]
  (log/debug "mount/render unkown-page")
  [:div
   [:h1 "What?! Where the hell am I?"]
   [event-state-watch Φ]
   [:button {:on-click  #(change-history! {:path (b/path-for routes :home)})}
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
    (send! {:message [:users/me {}]}))


  (case @!handler
    :home [home-page Φ]
    :auth-with-github [auth-with-github-page Φ]
    :init [init-page Φ]
    :unknown [unknown-page Φ]))
