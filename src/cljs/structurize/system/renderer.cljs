(ns structurize.system.renderer
  (:require [structurize.components.root-component :refer [root]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn render-root! [Φ]
  (r/render [root Φ] (js/document.getElementById "js-root")))


(defrecord Renderer [config-opts state side-effect-bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising renderer")
    (render-root! {:config-opts config-opts
                   :+tooling (:+tooling state)
                   :+app (:+app-track state)
                   :track (:track state)
                   :side-effect! (:side-effect! side-effect-bus)})
    component)

  (stop [component] component))
