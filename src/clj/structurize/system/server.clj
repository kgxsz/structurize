(ns structurize.system.server
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as log]))


(defrecord Server [config-opts handler]
  component/Lifecycle

  (start [component]
    (let [port (get-in config-opts [:server :port])
          stop-server (run-server (:handler handler) {:port port})]
      (log/info "Starting server on port" port)
      (assoc component :stop-server stop-server)))

  (stop [component]
    (when-let [stop-server (:stop-server component)]
      (log/info "Stopping server")
      (stop-server))
    (assoc component :stop-server nil)))
