(ns structurize.system
  (:require [structurize.system.chsk-conn :as chsk-conn]
            [structurize.env :refer [env]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]))


(defonce ^:private config-opts
  {:general {:github-auth-url (env :github-auth-url)
             :github-auth-client-id (env :github-auth-client-id)
             :something (env :something "default")}
   :chsk-conn chsk-conn/config-opts})


(defonce ^:private !app-state
  (r/atom {:secondary-view {:click-count 0}}))


(defn make-system [config-opts]
  (component/system-map
   :config-opts config-opts
   :!app-state !app-state
   :chsk-conn (chsk-conn/make-chsk-conn (:chsk-conn config-opts))))


(defonce system (component/start (make-system config-opts)))
