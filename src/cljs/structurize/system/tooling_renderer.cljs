(ns structurize.system.tooling-renderer
  (:require [structurize.components.tooling :refer [tooling]]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

(defn render-tooling [Φ]
  (r/render [tooling Φ] (js/document.getElementById "js-tooling")))


(defrecord ToolingRenderer [config-opts state browser comms]
  component/Lifecycle

  (start [component]
    (log/info "initialising tooling renderer")
    (render-tooling ^:tooling? {:config-opts config-opts
                                :!app-state (:!app-state state)
                                :!tooling-state (:!tooling-state state)
                                :history (:history browser)
                                :chsk (:chsk comms)
                                :chsk-state (:chsk-state comms)
                                :chsk-send (:chsk-send comms)})
    component)

  (stop [component] component))
