(ns structurize.system.renderer
  (:require [structurize.components.root-component :refer [root]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn render-root! [Φ]
  (r/render [root Φ] (js/document.getElementById "root")))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Renderer [config-opts state bus comms]
  component/Lifecycle

  (start [component]
    (log/info "Initialising renderer")
    (render-root! {:config-opts config-opts :state state :bus bus :comms comms})
    component)

  (stop [component] component))
