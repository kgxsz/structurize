(ns structurize.system
  (:require [structurize.system.side-effector :refer [map->SideEffector]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [structurize.system.browser :refer [map->Browser]]
            [structurize.system.comms :refer [map->Comms]]
            [structurize.system.renderer :refer [map->Renderer]]
            [structurize.system.state :refer [map->State]]
            [com.stuartsierra.component :as component]))


(defn make-system []
  (-> (component/system-map
       :config-opts (map->ConfigOpts {})
       :browser (map->Browser {})
       :comms (map->Comms {})
       :side-effector (map->SideEffector {})
       :state (map->State {})
       :renderer (map->Renderer {}))
      (component/system-using
       {:state [:config-opts]
        :browser [:config-opts :state]
        :comms [:config-opts :state]
        :side-effector [:config-opts :browser :comms :state]
        :renderer [:config-opts :state :side-effector]})))


(defonce system (component/start (make-system)))
