(ns structurize.core
  (:require [structurize.env :refer [env]]
            [structurize.system :refer [system]]
            [cemerick.url :refer [url]]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;; View ;;;;;;;;;;;;;;;;;;;;;;;;;


(defn tertiary-view []
  (log/debug "mount tertiary view")
  (log/debug "app-state:" (-> system :app-state :app-state deref))
  (log/debug "chsk-conn:" (-> system :chsk-conn :chsk-conn :state deref))
  [:div "tertiary view"])

(defn secondary-view []
  (log/debug "mount secondary view")
  [:div
   [:p "secondary view"]
   [tertiary-view]])

(defn root-view []
  (log/debug "mount root view")
  [:div
   [:h1 "Front end ready!"]
   [:h3 "more to come... "]
   [secondary-view]
   [:button
    [:a
     {:href (-> (url (:github-auth-url env))
                (assoc :query {:client_id (:github-auth-client-id env)})
                (str))}
     "Connect with Github!"]]])


(defn render-root! []
  (r/render [root-view] (js/document.getElementById "root")))


