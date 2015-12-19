(ns structurize.system
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]
            [bidi.ring :as br]
            [taoensso.timbre :refer [info]]))


(def routes ["/" {"default" :default}])

(def handlers {:default (fn [request] {:status 200 :body "Hello World!"})})

(defrecord Server [port]
  component/Lifecycle

  (start [component]
    (info "Starting server on port" port)
    (let [handler (br/make-handler routes handlers)
          stop-server-fn (run-server handler {:port port :join? false})]
      (assoc component :stop-server stop-server-fn)))

  (stop [component]
    (when-let [stop-server (:stop-server component)]
      (info "Stopping server")
      (stop-server))
    component))


(defn make-server []
  (map->Server {:port 3000}))


(defn make-system []
  (component/system-map
   :server (make-server)))
