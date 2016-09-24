(ns structurize.system
  (:require [structurize.system.config-opts :refer [map->ConfigOpts]]
            [structurize.system.browser :refer [map->Browser]]
            [structurize.system.comms :refer [map->Comms]]
            [structurize.system.app-renderer :refer [map->AppRenderer]]
            [structurize.system.tooling-renderer :refer [map->ToolingRenderer]]
            [structurize.system.state :refer [map->State]]
            [com.stuartsierra.component :as component]))


(defn make-system []
  (-> (component/system-map
       :config-opts (map->ConfigOpts {})
       :state (map->State {})
       :browser (map->Browser {})
       :comms (map->Comms {})
       :app-renderer (map->AppRenderer {})
       :tooling-renderer (map->ToolingRenderer {}))
      (component/system-using
       {:state [:config-opts]
        :browser [:config-opts :state]
        :comms [:config-opts :state]
        :app-renderer [:config-opts :state :browser :comms]
        :tooling-renderer [:config-opts :state :browser :comms]})))


(defonce system (component/start (make-system)))
