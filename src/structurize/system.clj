(ns structurize.system
  (:require [bidi.ring :as br]
            [clojure.pprint :as pprint]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [ring.util.response :refer [response status content-type]]
            [hiccup.page :refer [html5]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :refer [info]]))


(def page
  (html5
   [:head
    [:title "Structurize"]]
   [:body
    [:div
     [:h1 "Allo World!"]]]))

(def routes ["/" {"a" :a
                  "b" :b
                  true :page}])

(def handlers {:a (fn [request] (response "a?"))
               :b (fn [request] (response "b?"))
               :page (fn [request] (-> page response (content-type "text/html")))})


;;;;;;;;;;;; Handler goods ;;;;;;;;;;;;;;;

(defrecord Handler [config-opts]
  component/Lifecycle

  (start [component]
    (info "Starting handler event loop")
    (let [handler-fn (br/make-handler routes handlers)]
      (assoc component :handler-fn handler-fn)))

  (stop [component]
    (info "Stopping handler event loop")
    component))


(defn make-handler [config-opts]
  (map->Handler {:config-opts config-opts}))


;;;;;;;;;;;; Server goods ;;;;;;;;;;;;;;;

(defrecord Server [config-opts handler]
  component/Lifecycle

  (start [component]
    (info "Starting server on port" (:port config-opts))
    (let [stop-server-fn (run-server (:handler-fn handler) config-opts)]
      (assoc component :stop-server-fn stop-server-fn)))

  (stop [component]
    (when-let [stop-server-fn (:stop-server-fn component)]
      (info "Stopping server")
      (stop-server-fn))
    (dissoc component :stop-server-fn)))


(defn make-server [config-opts]
  (map->Server {:config-opts config-opts}))


;;;;;;;;;;;; System goods ;;;;;;;;;;;;;;;

(def system-config-opts
  {:server-config-opts {:port (env :port)}
   :handler-config-opts {}})


(defn make-system [config-opts]
  (-> (component/system-map
        :handler (make-handler (:handler-config-opts config-opts))
        :server (make-server (:server-config-opts config-opts)))
      (component/system-using
        {:server [:handler]})))
