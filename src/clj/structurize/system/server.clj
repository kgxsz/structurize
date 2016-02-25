(ns structurize.system.server
  (:require [bidi.ring :as br]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.defaults :as rmd]
            [taoensso.timbre :as log]))


(defn make-routes [handlers]
  ["/" {"chsk" {:get (:chsk-get-handler handlers)
                :post (:chsk-post-handler handlers)}
        "sign-in/github" {:post (:github-sign-in-handler handlers)}
        "sign-out" {:post (:sign-out-handler handlers)}
        true (:root-page-handler handlers)}])


(defrecord Server [config-opts db handlers]
  component/Lifecycle

  (start [component]
    (let [http-kit-opts (get-in config-opts [:server :http-kit-opts])
          middleware-opts (get-in config-opts [:server :middleware-opts])
          handler (-> (make-routes handlers)
                      br/make-handler
                      (rmd/wrap-defaults middleware-opts))
          stop-server (run-server handler http-kit-opts)]

      (log/info "starting server on port" (:port http-kit-opts))
      (assoc component :stop-server stop-server)))

  (stop [component]
    (when-let [stop-server (:stop-server component)]
      (log/info "stopping server")
      (stop-server))
    (assoc component :stop-server nil)))
