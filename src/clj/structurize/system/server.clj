(ns structurize.system.server
  (:require [bidi.ring :as br]
            [clj-time.core :as time]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [hiccup.page :refer [html5 include-js]]
            [org.httpkit.client :as http]
            [camel-snake-kebab.core :as csk]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.defaults :as rmd]
            [ring.util.response :refer [response content-type]]
            [taoensso.timbre :as log]))


(defn root-page-handler [request]
  (let [root-page (html5
                   [:head
                    [:title "Structurize"]]
                   [:body
                    [:div#root
                     [:h1 "Loading your stuff!"]]
                    (include-js "/js/structurize.js")
                    [:script {:type "text/javascript"} "structurize.runner.start();"]])]
    (-> root-page response (content-type "text/html"))))


(defn make-github-sign-in-handler
  "Returns a request handler function that carries out a GitHub oauth sign in given the
   code and the attempt-id. If successful, the user-id is put into the session."
  [config-opts db]

  (fn [{:keys [session params]}]
    (let [{:keys [code attempt-id]} params
          attempt (get-in @db [:sign-in-with-github attempt-id])]

      (if attempt

        (let [client-id (get-in config-opts [:general :github-auth-client-id])
              client-secret (get-in config-opts [:general :github-auth-client-secret])
              {:keys [status body error]} @(http/request {:url "https://github.com/login/oauth/access_token"
                                                          :method :post
                                                          :headers {"Accept" "application/json"}
                                                          :query-params {"client_id" client-id
                                                                         "client_secret" client-secret
                                                                         "code" code}
                                                          :timeout 5000})]

          (log/debug "confirming GitHub sign in for attempt:" attempt-id)

          (if (= 200 status)
            (let [{:keys [access-token scope]} (json/read-str body :key-fn csk/->kebab-case-keyword)
                  {:keys [status body error]} @(http/request {:url "https://api.github.com/user"
                                                              :method :get
                                                              :oauth-token access-token
                                                              :headers {"Accept" "application/json"}
                                                              :timeout 10000})]

              (if (= scope (get-in config-opts [:general :github-auth-scope]))

                (if (= 200 status)

                  (let [user-data (json/read-str body :key-fn csk/->kebab-case-keyword)
                        uid (:id user-data)]
                    (log/debug "GitHub sign in successful for attempt:" attempt-id)
                    (swap! db assoc-in [:sign-in-with-github attempt-id :confirmed-at] (time/now))
                    (swap! db assoc-in [:users uid] user-data)
                    {:status 200 :session (assoc session :uid uid)})

                  (do (log/infof "api request to GitHub failed for sign in attempt %s, with status %s, and error %s %s :" attempt-id status error body)
                      (swap! db update-in [:sign-in-with-github attempt-id] assoc :failed-at (time/now) :error :api-request-failed)
                      {:status 401}))

                (do (log/info "GitHub auth scope is insufficient for sign in attempt" attempt-id)
                    (swap! db update-in [:sign-in-with-github attempt-id] assoc :failed-at (time/now) :error :auth-scope-insufficient)
                    {:status 401})))

            (do (log/infof "access token request to GitHub failed for sign in attempt %s, with status %s, and error %s %s :" attempt-id status error body)
                (swap! db update-in [:sign-in-with-github attempt-id] assoc :failed-at (time/now) :error :access-token-request-failed)
                {:status 401})))

        (do (log/info "unable to match a GitHub sign in attempt for attempt-id" attempt-id)
            {:status 401})))))


(defn sign-out-handler [request]
  {:status 200 :session nil})


(defn make-routes [config-opts db comms]
  ["/" {"chsk" {:get (:ajax-get-or-ws-handshake-fn comms)
                :post (:ajax-post-fn comms)}
        "sign-in/github" {:post (make-github-sign-in-handler config-opts db)}
        "sign-out" {:post sign-out-handler}
        true root-page-handler}])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Server [config-opts db comms]
  component/Lifecycle

  (start [component]
    (let [http-kit-opts (get-in config-opts [:server :http-kit-opts])
          middleware-opts (get-in config-opts [:server :middleware-opts])
          routes (make-routes config-opts db comms)
          handler (-> (br/make-handler routes)
                      (rmd/wrap-defaults middleware-opts))
          stop-server (run-server handler http-kit-opts)]

      (log/info "starting server on port" (:port http-kit-opts))
      (assoc component :stop-server stop-server)))

  (stop [component]
    (when-let [stop-server (:stop-server component)]
      (log/info "stopping server")
      (stop-server))
    (assoc component :stop-server nil)))
