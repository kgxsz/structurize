(ns structurize.core
  (:require [bidi.ring :as br]
            [clojure.pprint :as pprint]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [ring.util.response :refer [response status content-type]]
            [hiccup.page :refer [html5 include-js]]
            [ring.middleware.defaults :as rmd]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as log]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))


(defn event-msg-handler [{:keys [event send-fn ring-req ?reply-fn]}]
  (let [[id ?payload] event]
    (log/info "Received event!" id)
    (when (and (= :hello/world id)
               ?reply-fn)
      (?reply-fn [:hello/world "Well hello to you too"]))))


(def root-page
  (html5
   [:head
    [:title "Structurize"]]
   [:body
    [:div#root
     [:h1 "Loading your stuff!"]]
    (include-js "/js/structurize.js")
    [:script {:type "text/javascript"} "structurize.runner.start();"]]))


;;;;;;;;;;;; Handler goods ;;;;;;;;;;;;;;;


(defrecord Handler [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Starting handler event loop")
    (let [chsk (sente/make-channel-socket! sente-web-server-adapter {})
          stop-chsk-router (sente/start-chsk-router! (:ch-recv chsk) event-msg-handler)
          handler (-> (br/make-handler ["/" {"chsk" {:get (:ajax-get-or-ws-handshake-fn chsk)
                                                     :post (:ajax-post-fn chsk)}
                                             true (fn [request] (-> root-page response (content-type "text/html")))}])
                      (rmd/wrap-defaults (:middleware-opts config-opts)))]
      (assoc component :handler handler :stop-chsk-router stop-chsk-router)))

  (stop [component]
    (when-let [stop-chsk-router (:stop-chsk-router component)]
      (log/info "Stopping handler event loop")
      (stop-chsk-router))
    (assoc component :stop-chsk-router nil)))


(defn make-handler [config-opts]
  (map->Handler {:config-opts config-opts}))


;;;;;;;;;;;; Server goods ;;;;;;;;;;;;;;;

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


;;;;;;;;;;;; System goods ;;;;;;;;;;;;;;;

(def server-config-opts
  {:port (env :port)})

(def handler-config-opts
  {:middleware-opts {:params {:urlencoded true
                              :nested true
                              :keywordize true}
                     :security {:anti-forgery true
                                :xss-protection {:enable? true, :mode :block}
                                :frame-options :sameorigin
                                :content-type-options :nosniff}
                     :static {:resources "public"}
                     :responses {:not-modified-responses true
                                 :absolute-redirects true
                                 :content-types true
                                 :default-charset "utf-8"}}})

(def system-config-opts
  {:server-config-opts server-config-opts
   :handler-config-opts handler-config-opts})


(defn make-system [config-opts]
  (-> (component/system-map
        :handler (make-handler (:handler-config-opts config-opts))
        :server (make-server (:server-config-opts config-opts)))
      (component/system-using
        {:server [:handler]})))
