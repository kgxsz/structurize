(ns structurize.system
  (:require [structurize.system.chsk-conn :refer [map->ChskConn]]
            [structurize.system.config-opts :refer [map->ConfigOpts]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]))

(defonce ^:private !app-state
  (r/atom {:secondary-view {:click-count 0}}))


(defn make-system []
  (-> (component/system-map
       :config-opts (map->ConfigOpts {})
       :!app-state !app-state
       :chsk-conn (map->ChskConn {}))
      (component/system-using
       {:chsk-conn [:config-opts]})))


(defonce system (component/start (make-system)))
