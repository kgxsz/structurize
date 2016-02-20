(ns structurize.system.comms
  (:require [clj-time.core :as time]
            [clojure.core.async :refer [go <! timeout]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :as log]))


(defn init-auth-with-github [{:keys [config-opts db]} [id ?data] ?reply-fn]
  (let [client-id (get-in config-opts [:general :github-auth-client-id])
        attempt-id (str (java.util.UUID/randomUUID))
        scope (get-in config-opts [:general :github-auth-scope])]

    (log/debug "initialising GitHub auth for attempt:" attempt-id)
    (swap! db assoc-in [:auth-with-github attempt-id] {:initialised-at (time/now)})
    (go (<! (timeout 1000)) (?reply-fn [id {:attempt-id attempt-id :client-id client-id :scope scope}]))))


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [φ]

  (fn [{:keys [event id ?data send-fn uid ring-req ?reply-fn client-id]}]
    (log/debugf "received message: %s from client: %s with uid: %s" id client-id uid)

    (case id
      :auth/init-auth-with-github (init-auth-with-github φ event ?reply-fn)
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
