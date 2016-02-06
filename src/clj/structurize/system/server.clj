(ns structurize.system.server
  (:require [bidi.ring :as br]
            [com.stuartsierra.component :as component]
            [hiccup.page :refer [html5 include-js]]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.defaults :as rmd]
            [ring.util.response :refer [response content-type]]
            [taoensso.timbre :as log]))


(def root-page
  (html5
   [:head
    [:title "Structurize"]]
   [:body
    [:div#root
     [:h1 "Loading your stuff!"]]
    (include-js "/js/structurize.js")
    [:script {:type "text/javascript"} "structurize.runner.start();"]]))


(defn make-routes [comms]
  ["/" {"chsk" {:get (:ajax-get-or-ws-handshake-fn comms)
                :post (:ajax-post-fn comms)}
        true (fn [request] (-> root-page response (content-type "text/html")))}])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Server [config-opts comms]
  component/Lifecycle

  (start [component]
    (let [http-kit-opts (get-in config-opts [:server :http-kit-opts])
          middleware-opts (get-in config-opts [:server :middleware-opts])
          handler (-> (br/make-handler (make-routes comms))
                      (rmd/wrap-defaults middleware-opts))
          stop-server (run-server handler http-kit-opts)]

      (log/info "Starting server on port" (:port http-kit-opts))
      (assoc component :stop-server stop-server)))

  (stop [component]
    (when-let [stop-server (:stop-server component)]
      (log/info "Stopping server")
      (stop-server))
    (assoc component :stop-server nil)))
