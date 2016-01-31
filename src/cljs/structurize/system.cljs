(ns structurize.system
  (:require [structurize.system.chsk-conn :refer [map->ChskConn]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [structurize.system.renderer :refer [map->Renderer]]
            [structurize.system.state :refer [map->State]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]))



(defn make-system []
  (-> (component/system-map
       :config-opts (map->ConfigOpts {})
       :chsk-conn (map->ChskConn {})
       :state (map->State {})
       :renderer (map->Renderer {}))
      (component/system-using
       {:chsk-conn [:config-opts]
        :state [:config-opts]
        :renderer [:state :chsk-conn]})))


(defonce system (component/start (make-system)))
