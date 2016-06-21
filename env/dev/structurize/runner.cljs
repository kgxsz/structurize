(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.renderer :as renderer]))


(defn reload! []
  (renderer/render-root! {:config-opts (:config-opts system)
                          :track (get-in system [:state :track])
                          :emit-side-effect! (get-in system [:side-effect-bus :emit-side-effect!])}))


(defn ^:export start []
  (enable-console-print!))
