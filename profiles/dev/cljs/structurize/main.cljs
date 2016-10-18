(ns ^:figwheel-no-load structurize.main
  (:require [structurize.system :refer [system]]
            [structurize.system.app-renderer :as app-renderer]
            [structurize.system.tooling-renderer :as tooling-renderer]
            [cljsjs.lodash]
            [cljsjs.d3]
            [cljsjs.textures]
            [d3.voronoi]))

(defn reload []
  (let [φ {:config-opts (:config-opts system)
           :!app-state (get-in system [:state :!app-state])
           :!tooling-state (get-in system [:state :!tooling-state])
           :history (get-in system [:browser :history])
           :chsk (get-in system [:comms :chsk])
           :chsk-state (get-in system [:comms :chsk-state])
           :chsk-send (get-in system [:comms :chsk-send])} ]
    (app-renderer/render-app φ)
    (tooling-renderer/render-tooling (with-meta φ {:tooling? true}))))


(defn ^:export start []
  (enable-console-print!))
