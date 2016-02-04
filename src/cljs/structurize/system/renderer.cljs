(ns structurize.system.renderer
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [structurize.components.root-component :refer [root]]
            [taoensso.timbre :as log]))


(defn render-root! [Φ]
  (r/render [root Φ] (js/document.getElementById "root")))


(defrecord Renderer [config-opts state bus comms]
  component/Lifecycle

  (start [component]
    (log/info "Initialising renderer")
    (render-root! {:config-opts config-opts :state state :bus bus :comms comms})
    component)

  (stop [component] component))
