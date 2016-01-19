(ns structurize.render.root-component
  (:require [structurize.system :refer [system]]
            [cemerick.url :refer [url]]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; other components


(defn tertiary-view []
  (log/debug "mount tertiary view")
  [:div "tertiary view"])


(defn secondary-view []
  (log/debug "mount secondary view")
  (let [!secondary-view-cursor (r/cursor (:!app-state system) [:secondary-view])]
    (log/debug "config-opts:" (:config-opts system))
    (log/debug "app-state:" @(:!app-state system))
    [:div
     [:p "secondary view"]
     [:button
      {:on-click #(swap! !secondary-view-cursor update :click-count inc)}
      (str "clicks: " (:click-count @!secondary-view-cursor))]
     [tertiary-view]]))


(defn login-with-github-component []
  (log/info "mount login-with-github-component")
  [:button
   [:a
    {:href (-> (url (get-in system [:config-opts :general :github-auth-url]))
               (assoc :query {:client_id (get-in system [:config-opts :general :github-auth-client-id])})
               str)}
    "Login with Github!"]])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; root component


(defn root-view []
  (log/debug "mount root view")
  [:div
   [:h1 "Front end ready!"]
   [:h3 "more to come... "]
   [secondary-view]
   [login-with-github-component]])

(defn root-controller []
  [root-view])
