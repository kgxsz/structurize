(ns structurize.core
  (:require [bidi.ring :as br]
            [clojure.pprint :as pprint]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [ring.util.response :refer [response status content-type]]
            [hiccup.page :refer [html5 include-js]]
            [ring.middleware.defaults :as rd]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as log]))

(def root
  (html5
   [:head
    [:title "Structurize"]]
   [:body
    [:div#root
     [:h1 "Loading your stuff!"]]
    (include-js "/js/structurize.js")
    [:script {:type "text/javascript"} "structurize.main.start();"]]))

(def routes ["/" {"a" :a
                  "b" :b
                  true :root}])

(def handlers {:a (fn [request] (response "a?"))
               :b (fn [request] (response "b?"))
               :root (fn [request] (-> root response (content-type "text/html")))})


;;;;;;;;;;;; Handler goods ;;;;;;;;;;;;;;;

(defrecord Handler [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Starting handler event loop")
    (let [handler-fn (br/make-handler routes handlers)
          handler-fn (-> handler-fn (rd/wrap-defaults (:middleware-opts config-opts)))]
      (assoc component :handler-fn handler-fn)))

  (stop [component]
    (log/info "Stopping handler event loop")
    component))


(defn make-handler [config-opts]
  (map->Handler {:config-opts config-opts}))


;;;;;;;;;;;; Server goods ;;;;;;;;;;;;;;;

(defrecord Server [config-opts handler]
  component/Lifecycle

  (start [component]
    (log/info "Starting server on port" (:port config-opts))
    (let [stop-server-fn (run-server (:handler-fn handler) config-opts)]
      (assoc component :stop-server-fn stop-server-fn)))

  (stop [component]
    (when-let [stop-server-fn (:stop-server-fn component)]
      (log/info "Stopping server")
      (stop-server-fn))
    (dissoc component :stop-server-fn)))


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
