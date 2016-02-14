(ns structurize.components.root-component
  (:require [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
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
                          {:keys [send! emit-event! change-history!]} :side-effector}]

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
        :received "logged in!"
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


(defn auth-with-github-page [{{:keys [!query]} :state
                              {:keys [send! change-history!]} :side-effector
                              :as Φ}]
  (log/debug "mount/render auth-with-github-page")

  (let [{:keys [state code error] :as ?payload} (select-keys @!query [:state :code :error])]
    (when (or (and state code) error)
      (send! {:message [:auth/confirm-auth-with-github ?payload]})
      (change-history! {:query {} :replace? true})))

  [:div
   [:h1 "We're authorizing you with github!"]
   [event-state-watch Φ]
   [:button {:on-click  #(change-history! {:path (b/path-for routes :home)})}
    "Go home!"]])


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


(defn root [{{:keys [!handler]} :state :as Φ}]
  (log/debug "mount/render root-component")
  (case @!handler
    :home [home-page Φ]
    :auth-with-github [auth-with-github-page Φ]
    :unknown [unknown-page Φ]
    [init-page Φ]))
