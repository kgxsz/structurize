(ns structurize.system
  (:require [structurize.system.comms :refer [map->Comms]]
            [structurize.system.server :refer [map->Server]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [com.stuartsierra.component :as component]))


(defn make-system []
  (-> (component/system-map
        :config-opts (map->ConfigOpts {})
        :comms (map->Comms {})
        :server (map->Server {}))
      (component/system-using
       {:comms [:config-opts]
        :server [:config-opts :comms]})))
