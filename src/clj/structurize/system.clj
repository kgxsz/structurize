(ns structurize.system
  (:require [structurize.system.comms :refer [map->Comms]]
            [structurize.system.server :refer [map->Server]]
            [structurize.system.handlers :refer [map->Handlers]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [com.stuartsierra.component :as component]))


(defn make-system []
  (-> (component/system-map
        :config-opts (map->ConfigOpts {})
        :db (atom {:auth-with-github {}})
        :comms (map->Comms {})
        :handlers (map->Handlers {})
        :server (map->Server {}))
      (component/system-using
       {:comms [:config-opts :db]
        :handlers [:config-opts :db :comms]
        :server [:config-opts :db :handlers]})))
