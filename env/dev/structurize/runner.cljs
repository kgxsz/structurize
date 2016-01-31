(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.system :refer [system]]
            [structurize.system.renderer :as renderer]))


(defn reload! []
  (renderer/render-root! (select-keys system [:config-opts :state :<event])))


(defn ^:export start []
  (enable-console-print!))
