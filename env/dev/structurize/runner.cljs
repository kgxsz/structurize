(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.renderer :as renderer]))


(defn reload! []
  (renderer/render-root! (assoc
                          (select-keys system [:config-opts :state :side-effector])
                          :emit-side-effect! (get-in system [:side-effector :emit-side-effect!]))))


(defn ^:export start []
  (enable-console-print!))
