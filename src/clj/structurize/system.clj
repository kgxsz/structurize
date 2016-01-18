(ns structurize.system
  (:require [structurize.system.chsk-conn :refer [map->ChskConn]]
            [structurize.system.handler :refer [map->Handler]]
            [structurize.system.server :refer [map->Server]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [com.stuartsierra.component :as component]))


(defn make-system []
  (-> (component/system-map
        :config-opts (map->ConfigOpts {})
        :chsk-conn (map->ChskConn {})
        :handler (map->Handler {})
        :server (map->Server {}))
      (component/system-using
       {:server [:config-opts :handler]
        :handler [:config-opts :chsk-conn]
        :chsk-conn [:config-opts]})))
