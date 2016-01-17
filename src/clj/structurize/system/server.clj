(ns structurize.system.server
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as log]))


(def config-opts {:port (env :port)})


(defrecord Server [config-opts handler]
  component/Lifecycle

  (start [component]
    (log/info "Starting server on port" (:port config-opts))
    (let [stop-server (run-server (:handler handler) config-opts)]
      (assoc component :stop-server stop-server)))

  (stop [component]
    (when-let [stop-server (:stop-server component)]
      (log/info "Stopping server")
      (stop-server))
    (assoc component :stop-server nil)))


(defn make-server [config-opts]
  (map->Server {:config-opts config-opts}))

