(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :as log]
            [clojure.core.async :refer [go <! timeout]]))


(defn init-github-auth [config-opts id ?reply-fn]
  (let [client-id (get-in config-opts [:general :github-auth-client-id])
        attempt-id (str (java.util.UUID/randomUUID))]
    (go (<! (timeout 1000)) (?reply-fn [id {:attempt-id attempt-id :client-id client-id}]))))


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [config-opts]

  (fn [{:keys [send-fn ring-req ?reply-fn], [id ?payload] :event}]
    (log/debug "received message:" id)

    (case id
      :auth/init-github-auth (init-github-auth config-opts id ?reply-fn)
      :chsk/ws-ping nil
      :chsk/uidport-open nil
      :chsk/uidport-close nil
      (log/error "failed to process message:" id))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [chsk-conn (sente/make-channel-socket! sente-web-server-adapter {})
          stop-chsk-router! (sente/start-chsk-router! (:ch-recv chsk-conn) (make-receive config-opts))]
      (assoc component
             :ajax-get-or-ws-handshake-fn (:ajax-get-or-ws-handshake-fn chsk-conn)
             :ajax-post-fn (:ajax-post-fn chsk-conn)
             :stop-chsk-router! stop-chsk-router!)))

  (stop [component]
    (when-let [stop-chsk-router! (:stop-chsk-router! component)]
      (log/info "stopping comms")
      (stop-chsk-router!))
    component))
