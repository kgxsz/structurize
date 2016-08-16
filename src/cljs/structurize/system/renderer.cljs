(ns structurize.system.renderer
  (:require [structurize.components.root-component :refer [root]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn render-root! [Φ]
  (r/render [root Φ] (js/document.getElementById "js-root")))


(defrecord Renderer [config-opts state side-effect-bus browser comms]
  component/Lifecycle

  (start [component]
    (log/info "initialising renderer")
    (render-root! {:config-opts config-opts
                   :!state (:!state state)
                   :<side-effects (:<side-effects side-effect-bus)
                   :history (:history browser)
                   :chsk (:chsk comms)
                   :chsk-state (:chsk-state comms)
                   :chsk-send (:chsk-send comms)})
    component)

  (stop [component] component))
