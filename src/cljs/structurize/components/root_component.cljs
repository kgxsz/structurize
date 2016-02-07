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
   {:on-click #(emit-event! [:inc-click-count/a {:cursor :!click-count-a, :Δ inc}])}
   (str "clicks: " @!click-count-a)])


(defn click-counter-b [{{:keys [!click-count-b]} :state
                        {:keys [emit-event!]} :side-effector}]

  (log/debug "mount/render click-counter-b")
  [:button
   {:on-click #(emit-event! [:inc-click-count/b {:cursor :!click-count-b, :Δ inc}])}
   (str "clicks: " @!click-count-b)])


(defn login-with-github [{{:keys [general]} :config-opts
                          {:keys [!core]} :state
                          {:keys [send! emit-event! change-history!]} :side-effector}]

  (log/debug "mount/render login-with-github")

  (let [!message-status (r/cursor !core [:message-status :auth/init-github-auth])
        !message-reply (r/cursor !core [:message-reply :auth/init-github-auth])]

    (when (= :received @!message-status)
      (let [{:keys [client-id attempt-id]} @!message-reply
            redirect-uri (str (:host general) (b/path-for routes :auth-github))]
        (change-history! {:prefix "https://github.com"
                          :path "/login/oauth/authorize"
                          :query {:client_id client-id
                                  :state attempt-id
                                  :redirect_uri redirect-uri}})))

    [:div
     [:button
      {:on-click (fn [] (send! {:message [:auth/init-github-auth {}]}))}
      (case @!message-status
        :sent "logging in!"
        :received "logged in!"
        "log in with GitHub")]]))


(defn event-state-watch [{:keys [state]}]
  (log/debug "mount/render event-state-watch")
  [:span (str @(:!core state))])


(defn root [{{:keys [!handler]} :state
             {:keys [change-history!]} :side-effector
             :as Φ}]
  (log/debug "mount/render root-component")

  (case @!handler
    :home [:div
           [:h1 "Front end ready!"]
           [click-counter-a Φ]
           [click-counter-b Φ]
           [login-with-github Φ]
           [event-state-watch Φ]
           [:button {:on-click  #(change-history! {:path (b/path-for routes :foo)
                                                   :query {:a 1, :b 2}})}
            "update foo"]
           [:button {:on-click  #(change-history! {:path (b/path-for routes :bar)})}
            "update bar"]
           [:button {:on-click #(change-history! {:query {:y "keigo+&1"}
                                                  :replace? true})}
            "nuke params"]]

    :github-auth [:div
                  [:h3 "github auth confirmation"]]

    [:div [:h2 "Couldn't find this goshdarn page"]]))
