(ns structurize.main
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [structurize.system :refer [make-system]]))

(def system (make-system))

(defn start! []
  (alter-var-root #'system component/start))

(defn stop! []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn reload! [& {:keys [refresh-all?]}]
  (stop!)
  (if refresh-all?
    (refresh-all :after 'structurize.main/start!)
    (refresh :after 'structurize.main/start!)))
