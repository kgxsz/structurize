(ns structurize.system
  (:require [structurize.system.side-effector :refer [map->SideEffector]]
            [structurize.system.side-effect-bus :refer [map->SideEffectBus]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [structurize.system.browser :refer [map->Browser]]
            [structurize.system.comms :refer [map->Comms]]
            [structurize.system.renderer :refer [map->Renderer]]
            [structurize.system.state :refer [map->State]]
            [com.stuartsierra.component :as component]))


(defn make-system []
  (-> (component/system-map
       :config-opts (map->ConfigOpts {})
       :side-effect-bus (map->SideEffectBus {})
       :state (map->State {})
       :browser (map->Browser {})
       :comms (map->Comms {})
       :side-effector (map->SideEffector {})
       :renderer (map->Renderer {}))
      (component/system-using
       {:side-effect-bus [:config-opts]
        :state [:config-opts]
        :browser [:config-opts :side-effect-bus]
        :comms [:config-opts :side-effect-bus]
        :side-effector [:config-opts :side-effect-bus :browser :comms :state]
        :renderer [:config-opts :state :side-effect-bus]})))


(defonce system (component/start (make-system)))
