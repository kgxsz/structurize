(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.app-renderer :as app-renderer]
            [structurize.system.tooling-renderer :as tooling-renderer]
            [cljsjs.lodash]
            [cljsjs.d3]
            [cljsjs.textures]))

(defn reload []
  (let [φ {:config-opts (:config-opts system)
           :!state (get-in system [:state :!state])
           :history (get-in system [:browser :history])
           :chsk (get-in system [:comms :chsk])
           :chsk-state (get-in system [:comms :chsk-state])
           :chsk-send (get-in system [:comms :chsk-send])} ]
    (app-renderer/render-app φ)
    (tooling-renderer/render-tooling (assoc φ :context {:tooling? true}))))


(defn ^:export start []
  (enable-console-print!))
