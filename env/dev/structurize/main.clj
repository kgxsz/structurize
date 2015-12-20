(ns structurize.main
  (:require [structurize.core :refer [make-system system-config-opts]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [com.stuartsierra.component :as component]))


(def system (make-system system-config-opts))

(defn start! []
  (alter-var-root #'system component/start))

(defn stop! []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn reload! [& {:keys [refresh-all?]}]
  (stop!)
  (if refresh-all?
    (refresh-all :after 'structurize.main/start!)
    (refresh :after 'structurize.main/start!)))
