(ns structurize.system
  (:require [structurize.env :refer [env]]
            [structurize.event-msg-handler :refer [event-msg-handler]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; system components


(defrecord ChskConn [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Initialising channel socket connection")
    (let [chsk-conn (sente/make-channel-socket! "/chsk" {:type (:type config-opts)})]
      (sente/start-chsk-router! (:ch-recv chsk-conn) event-msg-handler)
      (assoc component :chsk-conn chsk-conn)))

  (stop [component] component))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; system


(defonce config-opts
  {:general {:github-auth-url (env :github-auth-url)
             :github-auth-client-id (env :github-auth-client-id)}
   :chsk-conn {:type :auto}})


(defn make-system []
  (component/system-map
   :config-opts config-opts
   :!app-state (r/atom {:secondary-view {:click-count 0}})
   :chsk-conn (map->ChskConn {:config-opts (:chsk-conn config-opts)})))


(defonce system (component/start (make-system)))
