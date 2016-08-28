(ns structurize.main
  (:require [structurize.system :as system]
            [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
            [com.stuartsierra.component :as component]))

(def system (system/make-system))

(defn start! []
  (alter-var-root #'system component/start))

(defn stop! []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn reload! [& {:keys [refresh-all?]}]
  (stop!)
  (set-refresh-dirs "clj")
  (if refresh-all?
    (refresh-all :after 'structurize.main/start!)
    (refresh :after 'structurize.main/start!)))
