(ns structurize.components.root-component
  (:require [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [cemerick.url :refer [url]]
            [reagent.core :as r]
            [bidi.bidi :as b]
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


(defn login-with-github [{{:keys [!core !host]} :state
                          {:keys [send! emit-event! change-history!]} :side-effector}]

  (log/debug "mount/render login-with-github")

  (let [!message-status (r/cursor !core [:message-status :auth/init-github-auth])
        !message-reply (r/cursor !core [:message-reply :auth/init-github-auth])]

    (when (= :received @!message-status)
      (let [{:keys [client-id state]} @!message-reply
            redirect-uri (str @!host (b/path-for routes :auth-github))
            location (-> (url "https://github.com")
                         (assoc :path "/login/oauth/authorize")
                         (assoc :query {:client_id client-id
                                        :state state
                                        :redirect_uri redirect-uri}))]
        (change-history! location {:leave? true})))

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


(defn root [{{:keys [change-history!]} :side-effector
             :as Φ}]
  (log/debug "mount/render root-component")
  [:div
   [:h1 "Front end ready!"]
   [click-counter-a Φ]
   [click-counter-b Φ]
   [login-with-github Φ]
   [event-state-watch Φ]
   [:button {:on-click  #(change-history! "/dfd")}
    "dfd"]
   [:button {:on-click #(change-history! "/dfg" {:replace? true})}
    "adsda"]
   [:a {:href "/ab"
        :on-click #(change-history! "a")}
    "go"]])
