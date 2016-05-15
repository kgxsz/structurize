(ns structurize.main
  (:gen-class)
  (:require [structurize.system :as system]
            [com.stuartsierra.component :as component]))

(defn -main [& args]
  (component/start (system/make-system)))
