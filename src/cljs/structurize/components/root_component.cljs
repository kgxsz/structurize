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
  [:div
   [:span (str @(:!global state))]
   [:button
    {:on-click (fn [] (send! {:message [:auth/login-with-github {}]}))}
    "Login with GitHub"

    ;; The send message has an ID, use this. Let your state include a thing that tracks the send-status for that id,
    ;; so it'll be in transit, or success, or failed. These can be generalised and used by any component that wants
    ;; to react on those situations. What we do with the returning data is something we'd want to define specifically
    ;; here. Put it in a cache if you want, we don't care, that's the point.


    ;; Compose the functions that dress the data and render them, make them independent of how I get the data down there.


    #_[:a
       {:href (-> (url (get-in system [:config-opts :general :github-auth-url]))
                  (assoc :query {:client_id (get-in system [:config-opts :general :github-auth-client-id])})
                  str)}
       "Login with Github!"]]])


(defn root [Φ]
  (log/debug "mount/render root-component")
  [:div
   [:h1 "Front end ready!"]
   [click-counter-a Φ]
   [click-counter-b Φ]
   [login-with-github Φ]])


;; Composed ->renderable
;; optimistic updates
;; forming the return data in the right way, and putting it somewhere intelligent.
