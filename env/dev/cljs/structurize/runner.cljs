(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.renderer :as renderer]
            [cljsjs.lodash]))


(defn reload []
  (renderer/render-root {:config-opts (:config-opts system)
                          :!state (get-in system [:state :!state])
                          :<side-effects (get-in system [:side-effector :<side-effects])
                          :history (get-in system [:browser :history])
                          :chsk (get-in system [:comms :chsk])
                          :chsk-state (get-in system [:comms :chsk-state])
                          :chsk-send (get-in system [:comms :chsk-send])}))


(defn ^:export start []
  (enable-console-print!))
