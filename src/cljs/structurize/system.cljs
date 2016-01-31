(ns structurize.system
  (:require [structurize.system.chsk-conn :refer [map->ChskConn]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [structurize.system.machine :refer [map->Machine]]
            [structurize.system.renderer :refer [map->Renderer]]
            [structurize.system.state :refer [map->State]]
            [cljs.core.async :as a]
            [com.stuartsierra.component :as component]))


(defn make-system []
  (-> (component/system-map
       :config-opts (map->ConfigOpts {})
       :<event (a/chan)
       :chsk-conn (map->ChskConn {})
       :state (map->State {})
       :machine (map->Machine {})
       :renderer (map->Renderer {}))
      (component/system-using
       {:chsk-conn [:config-opts]
        :state [:config-opts]
        :machine [:config-opts :<event :chsk-conn :state]
        :renderer [:config-opts :state :<event]})))


(defonce system (component/start (make-system)))
