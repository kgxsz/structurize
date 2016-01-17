(ns structurize.system
  (:require [structurize.system.chsk-conn :as chsk-conn]
            [structurize.system.handler :as handler]
            [structurize.system.server :as server]
            [com.stuartsierra.component :as component]))


(def config-opts
  {:general {}
   :chsk-conn chsk-conn/config-opts
   :handler handler/config-opts
   :server server/config-opts})


(defn make-system [config-opts]
  (-> (component/system-map
        :config-opts config-opts
        :chsk-conn (chsk-conn/make-chsk-conn (:chsk-conn config-opts))
        :handler (handler/make-handler (:handler config-opts))
        :server (server/make-server (:server config-opts)))
      (component/system-using
       {:handler [:chsk-conn]
        :server [:handler]})))
