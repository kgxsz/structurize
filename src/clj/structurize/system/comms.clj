(ns structurize.system.comms
  (:require [clj-time.core :as time]
            [clojure.core.async :refer [go <! timeout]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :as log]))


(defn init-sign-in-with-github [{:keys [config-opts db]} [id ?data] uid ?reply-fn]
  (let [client-id (get-in config-opts [:general :github-auth-client-id])
        attempt-id (str (java.util.UUID/randomUUID))
        scope (get-in config-opts [:general :github-auth-scope])]

    (log/debug "initialising GitHub sign in for attempt:" attempt-id)
    (swap! db assoc-in [:sign-in-with-github attempt-id] {:initialised-at (time/now) :client-id client-id})
    (go (<! (timeout 1000)) (?reply-fn [id {:attempt-id attempt-id :client-id client-id :scope scope}]))))


(defn me [{:keys [config-opts db]} [id ?data] uid ?reply-fn]
  (let [user (select-keys (get-in @db [:users uid]) [:name :email :login])]
    (go (<! (timeout 1000)) (?reply-fn [id {:user user}]))))


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [φ]

  (fn [{:keys [event id ?data send-fn uid ring-req ?reply-fn client-id]}]
    (log/debugf "received message: %s from client: %s with uid: %s" id client-id uid)

    (case id
      :sign-in/init-sign-in-with-github (init-sign-in-with-github φ event uid ?reply-fn)
      :users/me (me φ event uid ?reply-fn)
      :chsk/ws-ping nil
      :chsk/uidport-open nil
      :chsk/uidport-close nil
      (log/error "No handling function to process message:" id))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts db]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [chsk-conn (sente/make-channel-socket! sente-web-server-adapter (get-in config-opts [:comms :chsk-opts]))
          stop-chsk-router! (sente/start-chsk-router! (:ch-recv chsk-conn) (make-receive {:config-opts config-opts :db db}))]
      (assoc component
             ;; enumerate the http handlers here, server uses then judiciously
             ;; the ws handlers are multimethoded on the recevie function
             :ajax-get-or-ws-handshake-fn (:ajax-get-or-ws-handshake-fn chsk-conn)
             :ajax-post-fn (:ajax-post-fn chsk-conn)
             :stop-chsk-router! stop-chsk-router!)))

  (stop [component]
    (when-let [stop-chsk-router! (:stop-chsk-router! component)]
      (log/info "stopping comms")
      (stop-chsk-router!))
    component))
