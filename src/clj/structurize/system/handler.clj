(ns structurize.system.handler
  (:require [bidi.ring :as br]
            [com.stuartsierra.component :as component]
            [hiccup.page :refer [html5 include-js]]
            [ring.middleware.defaults :as rmd]
            [ring.util.response :refer [response content-type]]
            [taoensso.timbre :as log]))


(def config-opts {:middleware-opts {:params {:urlencoded true
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


(def root-page
  (html5
   [:head
    [:title "Structurize"]]
   [:body
    [:div#root
     [:h1 "Loading your stuff!"]]
    (include-js "/js/structurize.js")
    [:script {:type "text/javascript"} "structurize.runner.start();"]]))


(defrecord Handler [config-opts chsk-conn]
  component/Lifecycle

  (start [component]
    (log/info "Initialising handler")
    (let [handler (-> (br/make-handler ["/" {"chsk" {:get (:ajax-get-or-ws-handshake-fn chsk-conn)
                                                     :post (:ajax-post-fn chsk-conn)}
                                             true (fn [request] (-> root-page response (content-type "text/html")))}])
                      (rmd/wrap-defaults (:middleware-opts config-opts)))]
      (assoc component :handler handler)))

  (stop [component]
    (log/info "Stopping handler")
    (assoc component :handler nil)))


(defn make-handler [config-opts]
  (map->Handler {:config-opts config-opts}))
