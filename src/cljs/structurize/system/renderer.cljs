(ns structurize.system.renderer
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [structurize.components.root-component :refer [root-component]]
            [taoensso.timbre :as log]))


(defn render-root! [ctx]
  (r/render [root-component ctx] (js/document.getElementById "root")))


(defrecord Renderer [config-opts state chsk-conn]
  component/Lifecycle

  (start [component]
    (log/info "Initialising renderer")
    (render-root! {:config-opts config-opts :state state :chsk-conn chsk-conn})
    component)

  (stop [component] component))
