(ns structurize.system.renderer
  (:require [structurize.components.root-component :refer [root]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn render-root! [Φ]
  (r/render [root Φ] (js/document.getElementById "root")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Renderer [config-opts state side-effector]
  component/Lifecycle

  (start [component]
    (log/info "initialising renderer")
    (render-root! {:config-opts config-opts :state state :emit-side-effect! (:emit-side-effect! side-effector) :side-effector side-effector})
    component)

  (stop [component] component))
