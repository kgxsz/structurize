(ns structurize.main
  (:gen-class)
  (:require [structurize.system :as system]
            [com.stuartsierra.component :as component]))

(def system (system/make-system))

(defn start! []
  (alter-var-root #'system component/start))

(defn -main [& args]
  (start!))
