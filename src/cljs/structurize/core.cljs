(ns structurize.core
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
  (let [!app-state (-> system :!app-state)
        !secondary-view-cursor (r/cursor !app-state [:secondary-view])]
    (log/debug "config-opts:" (-> system :config-opts))
    (log/debug "app-state:" (-> system :!app-state deref))
    [:div
     [:p "secondary view"]
     [:button
      {:on-click #(swap! !secondary-view-cursor update :click-count inc)}
      (str "clicks: " (-> !secondary-view-cursor deref :click-count))]
     [tertiary-view]]))


(defn login-with-github-component []
  (log/info "mount login-with-github-component")
  [:button
   [:a
    {:href (-> (url (-> system :config-opts :general :github-auth-url))
               (assoc :query {:client_id (-> system :config-opts :general :github-auth-client-id)})
               (str))}
    "Login with Github!"]])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; root component


(defn root-view []
  (log/debug "mount root view")
  [:div
   [:h1 "Front end ready!"]
   [:h3 "more to come... "]
   [secondary-view]
   [login-with-github-component]])


(defn render-root! []
  (r/render [root-view] (js/document.getElementById "root")))
