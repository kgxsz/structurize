(ns structurize.system.renderer
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [structurize.components.root-component :refer [root-component]]
            [taoensso.timbre :as log]))


(defn render-root! [Δ]
  (r/render [root-component Δ] (js/document.getElementById "root")))


(defrecord Renderer [config-opts state <event]
  component/Lifecycle

  (start [component]
    (log/info "Initialising renderer")
    (render-root! {:config-opts config-opts :state state :<event <event})
    component)

  (stop [component] component))
