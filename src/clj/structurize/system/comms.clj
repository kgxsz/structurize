(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :as log]
            [clj-time.core :as time]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [camel-snake-kebab.core :as csk]
            [clojure.core.async :refer [go <! timeout]]))


(defn init-auth-with-github [{:keys [config-opts db]} [id ?data] ?reply-fn]
  (log/debug "initialising github auth")
  (let [client-id (get-in config-opts [:general :github-auth-client-id])
        attempt-id (str (java.util.UUID/randomUUID))
        scope (get-in config-opts [:general :github-auth-scope])]
    (swap! db update-in [:auth-with-github] conj {:attempt-id attempt-id :initialised-at (time/now)})
    (go (<! (timeout 1000)) (?reply-fn [id {:attempt-id attempt-id :client-id client-id :scope scope}]))))


(defn confirm-auth-with-github [{:keys [config-opts db]} [id ?data] ?reply-fn]
  (log/debug "confirming github auth")
  (let [{:keys [state code]} ?data
        attempt (first (filter #(= (:attempt-id %) state) (:auth-with-github @db)))]

    (println attempt)
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

        (if (= 200 status)
          (let [{:keys [access-token scope]} (json/read-str body :key-fn csk/->kebab-case-keyword)
                {:keys [status body error]} @(http/request {:url "https://api.github.com/user"
                                                            :method :get
                                                            :oauth-token access-token
                                                            :headers {"Accept" "application/json"}
                                                            :timeout 5000})]

            (if (= scope (get-in config-opts [:general :github-auth-scope]))

              (if (= 200 status)

                (let [user-data (json/read-str body :key-fn csk/->kebab-case-keyword)]
                  (swap! db update-in [:auth-with-github] conj (assoc attempt :confirmed-at (time/now)))
                  (?reply-fn [id {:user-data (select-keys user-data [:email :name :login :avatar-url])}]))

                (do
                  (log/warn "api request to GitHub failed:" status error body)
                  (swap! db update-in [:auth-with-github] conj (assoc attempt :failed-at (time/now) :error :api-request-failed))
                  (?reply-fn [id {:error :api-request-failed}])))

              (do
                (log/warn "scope does not match")
                (swap! db update-in [:auth-with-github] conj (assoc attempt :failed-at (time/now) :error :scope-does-not-match))
                (?reply-fn [id {:error :scope-does-not-match}]))))

          (do
            (log/warn "access token request to GitHub failed:" status error body)
            (swap! db update-in [:auth-with-github] conj (assoc attempt :failed-at (time/now) :error :access-token-request-failed))
            (?reply-fn [id {:error :access-token-request-failed}]))))

      (do
        (log/warn "unable to match state for GitHub authentication")
        (?reply-fn [id {:error :unable-to-match-state}])))))


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [φ]

  (fn [{:keys [event id ?data send-fn uid ring-req ?reply-fn client-id]}]
    (log/debug "received message:" id)

    (case id
      :auth/init-auth-with-github (init-auth-with-github φ event ?reply-fn)
      :auth/confirm-auth-with-github (confirm-auth-with-github φ event ?reply-fn)
      :chsk/ws-ping nil
      :chsk/uidport-open nil
      :chsk/uidport-close nil
      (log/error "failed to process message:" id))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts db]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [chsk-conn (sente/make-channel-socket! sente-web-server-adapter (get-in config-opts [:comms :chsk-opts]))
          stop-chsk-router! (sente/start-chsk-router! (:ch-recv chsk-conn) (make-receive {:config-opts config-opts :db db}))]
      (assoc component
             :ajax-get-or-ws-handshake-fn (:ajax-get-or-ws-handshake-fn chsk-conn)
             :ajax-post-fn (:ajax-post-fn chsk-conn)
             :stop-chsk-router! stop-chsk-router!)))

  (stop [component]
    (when-let [stop-chsk-router! (:stop-chsk-router! component)]
      (log/info "stopping comms")
      (stop-chsk-router!))
    component))
