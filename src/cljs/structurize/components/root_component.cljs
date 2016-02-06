(ns structurize.components.root-component
  (:require [cemerick.url :refer [url]]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn click-counter-a [{{:keys [!click-count-a]} :state, {:keys [emit-event!]} :bus}]
  (log/debug "mount/render click-counter-a")
  [:button
   {:on-click #(emit-event! [:inc-click-count/a {:cursor :!click-count-a, :Δ inc}])}
   (str "clicks: " @!click-count-a)])


(defn click-counter-b [{{:keys [!click-count-b]} :state, {:keys [emit-event!]} :bus}]
  (log/debug "mount/render click-counter-b")
  [:button
   {:on-click #(emit-event! [:inc-click-count/b {:cursor :!click-count-b, :Δ inc}])}
   (str "clicks: " @!click-count-b)])


(defn login-with-github [{:keys [state], {:keys [send!]} :comms}]
  (log/debug "mount/render login-with-github")
  (let [!message-status (r/cursor (:!core state) [:message-status :auth/init-github-auth])]
    [:div
     [:span (str @(:!core state))]
     [:button
      {:on-click (fn [] (send! {:message [:auth/init-github-auth {}]}))}
      (case @!message-status
        :sent "logging in!"
        :received "logged in!"
        "log in with GitHub")

      #_[:a
         {:href (-> (url (get-in system [:config-opts :general :github-auth-url]))
                    (assoc :query {:client_id (get-in system [:config-opts :general :github-auth-client-id])})
                    str)}
         "Login with Github!"]]]))


(defn root [Φ]
  (log/debug "mount/render root-component")
  [:div
   [:h1 "Front end ready!"]
   [click-counter-a Φ]
   [click-counter-b Φ]
   [login-with-github Φ]
   [:button {:on-click #(set! (.-location js/window) "https://google.com")}
    "google"]])


;; Composed ->renderable
;; optimistic updates
;; forming the return data in the right way, and putting it somewhere intelligent.
