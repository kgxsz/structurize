(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.renderer :as renderer]))


(defn reload! []
  (renderer/render-root! {:config-opts (:config-opts system)
                          :+tooling (get-in system [:state :+tooling])
                          :+app (get-in system [:state :+app-track])
                          :track (get-in system [:state :track])
                          :side-effect! (get-in system [:side-effect-bus :side-effect!])}))


(defn ^:export start []
  (enable-console-print!))
