(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.renderer :as renderer]))


(defn reload! []
  (renderer/render-root! {:config-opts (:config-opts system)
                          :!db (get-in system [:state :!db])
                          :emit-side-effect! (get-in system [:side-effector :emit-side-effect!])}))


(defn ^:export start []
  (enable-console-print!))
