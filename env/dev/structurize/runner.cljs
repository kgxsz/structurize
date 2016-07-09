(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.renderer :as renderer]))


(defn reload! []
  (renderer/render-root! {:config-opts (:config-opts system)
                          :track-app (get-in system [:state :track-app])
                          :track-tooling (get-in system [:state :track-tooling])
                          :side-effect! (get-in system [:side-effect-bus :side-effect!])}))


(defn ^:export start []
  (enable-console-print!))
