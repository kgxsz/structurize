(ns structurize.system.app-renderer
  (:require [structurize.components.app :refer [app]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn render-app [Φ]
  (r/render [app Φ] (js/document.getElementById "js-app")))


(defrecord AppRenderer [config-opts state side-effector browser comms]
  component/Lifecycle

  (start [component]
    (log/info "initialising app renderer")
    (render-app {:config-opts config-opts
                 :!state (:!state state)
                 :history (:history browser)
                 :chsk (:chsk comms)
                 :chsk-state (:chsk-state comms)
                 :chsk-send (:chsk-send comms)}) 
    component)

  (stop [component] component))
