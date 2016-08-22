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
       :state (map->State {})
       :browser (map->Browser {})
       :comms (map->Comms {})
       :side-effector (map->SideEffector {})
       :renderer (map->Renderer {}))
      (component/system-using
       {:state [:config-opts]
        :side-effector [:config-opts]
        :browser [:config-opts :state :side-effector]
        :comms [:config-opts :state :side-effector]
        :renderer [:config-opts :state :side-effector :browser :comms]})))


(defonce system (component/start (make-system)))
