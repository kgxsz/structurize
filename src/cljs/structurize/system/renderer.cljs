(ns structurize.system.renderer
  (:require [structurize.components.root-component :refer [root]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn render-root! [Φ]
  (r/render [root Φ] (js/document.getElementById "js-root")))


(defrecord Renderer [config-opts state side-effector browser comms]
  component/Lifecycle

  (start [component]
    (log/info "initialising renderer")
    (r/render [root {:config-opts config-opts
                     :!state (:!state state)
                     :<side-effects (:<side-effects side-effector)
                     :history (:history browser)
                     :chsk (:chsk comms)
                     :chsk-state (:chsk-state comms)
                     :chsk-send (:chsk-send comms)}]
              (js/document.getElementById "js-root"))
    component)

  (stop [component] component))
