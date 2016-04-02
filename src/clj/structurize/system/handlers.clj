(ns structurize.system.handlers
  (:require [camel-snake-kebab.core :as csk]
            [clj-time.core :as time]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [hiccup.page :refer [html5 include-js include-css]]
            [org.httpkit.client :as http]
            [ring.util.response :refer [response content-type]]
            [taoensso.timbre :as log]))


(def coffee-cup-svg
  [:svg {:version 1.1
         :xmlns "http://www.w3.org/2000/svg"
         :xmlns:xlink "http://www.w3.org/1999/xlink"
         :width 60
         :height 60
         :viewBox "0 0 20 20"}
   [:path {:fill "#F6F9FC"
           :d "M10 15c-1.654 0-3-1.346-3-3s1.346-3 3-3 3 1.346 3 3-1.346 3-3 3zM10 10c-1.103 0-2 0.897-2 2s0.897 2 2 2c1.103 0 2-0.897 2-2s-0.897-2-2-2z"}]
   [:path {:fill  "#F6F9FC"
           :d "M15.904 2.056l-0.177-0.707c-0.189-0.756-0.948-1.349-1.728-1.349h-8c-0.78 0-1.538 0.593-1.728 1.349l-0.177 0.707c-0.631 0.177-1.096 0.757-1.096 1.444v1c0 0.663 0.432 1.226 1.029 1.424l0.901 12.614c0.058 0.806 0.762 1.462 1.57 1.462h7c0.808 0 1.512-0.656 1.57-1.462l0.901-12.614c0.597-0.198 1.029-0.761 1.029-1.424v-1c0-0.687-0.464-1.267-1.096-1.444zM6 1h8c0.319 0 0.68 0.282 0.757 0.591l0.102 0.409h-9.719l0.102-0.409c0.077-0.309 0.438-0.591 0.757-0.591zM14.892 7h-9.783l-0.071-1h9.926l-0.071 1zM14.249 16h-8.497l-0.571-8h9.64l-0.571 8zM13.5 19h-7c-0.29 0-0.552-0.244-0.573-0.533l-0.105-1.467h8.355l-0.105 1.467c-0.021 0.289-0.283 0.533-0.573 0.533zM16 4.5c0 0.276-0.224 0.5-0.5 0.5h-11c-0.276 0-0.5-0.224-0.5-0.5v-1c0-0.275 0.224-0.499 0.499-0.5 0.001 0 0.001 0 0.002 0s0.002-0 0.003-0h10.997c0.276 0 0.5 0.224 0.5 0.5v1z"}]])


(defn root-page-handler [request]
  (let [root-page (html5
                   [:head
                    [:title "Structurize"]
                    [:meta {:name "viewport" :content "width = device-width, initial-scale = 1.0, user-scalable = no"}]
                    [:link {:type "text/css" :href "https://fonts.googleapis.com/css?family=Fira+Mono|Raleway:300" :rel "stylesheet"}]
                    (include-css "/css/icomoon.css")
                    (include-css "/css/style.css")]
                   [:body
                    [:div#root
                     [:div.page.init-page
                      coffee-cup-svg
                      [:h5.loading-caption "loading"]]]
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Handlers [config-opts db comms]
  component/Lifecycle

  (start [component]
    (log/info "initialising handlers")
    (let []
      (assoc component
             :chsk-get-handler (get-in comms [:chsk-conn :ajax-get-or-ws-handshake-fn])
             :chsk-post-handler (get-in comms [:chsk-conn :ajax-post-fn])
             :github-sign-in-handler (make-github-sign-in-handler config-opts db)
             :sign-out-handler sign-out-handler
             :root-page-handler root-page-handler)))

  (stop [component] component))
