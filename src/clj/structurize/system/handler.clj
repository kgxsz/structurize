(ns structurize.system.handler
  (:require [bidi.ring :as br]
            [com.stuartsierra.component :as component]
            [hiccup.page :refer [html5 include-js]]
            [ring.middleware.defaults :as rmd]
            [ring.util.response :refer [response content-type]]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; root rendering


(def root-page
  (html5
   [:head
    [:title "Structurize"]]
   [:body
    [:div#root
     [:h1 "Loading your stuff!"]]
    (include-js "/js/structurize.js")
    [:script {:type "text/javascript"} "structurize.runner.start();"]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Handler [config-opts chsk-conn]
  component/Lifecycle

  (start [component]
    (log/info "Initialising handler")
    (let [middleware-opts (get-in config-opts [:handler :middleware-opts])
          handler (-> (br/make-handler ["/" {"chsk" {:get (:ajax-get-or-ws-handshake-fn chsk-conn)
                                                     :post (:ajax-post-fn chsk-conn)}
                                             true (fn [request] (-> root-page response (content-type "text/html")))}])
                      (rmd/wrap-defaults middleware-opts))]
      (assoc component :handler handler)))

  (stop [component]
    (log/info "Stopping handler")
    (assoc component :handler nil)))
