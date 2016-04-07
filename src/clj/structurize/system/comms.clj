(ns structurize.system.comms
  (:require [clj-time.core :as time]
            [clojure.core.async :refer [go <! timeout]]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method handling


(defmulti handler (fn [_ event-message] (:id event-message)))


(defmethod handler :sign-in/init-sign-in-with-github
  [{:keys [config-opts db]} {:keys [uid ?reply-fn] [id ?data] :event}]

  (let [client-id (get-in config-opts [:github-auth :client-id])
        attempt-id (str (java.util.UUID/randomUUID))
        scope (get-in config-opts [:github-auth :scope])]

    (log/debug "initialising GitHub sign in for attempt:" attempt-id)
    (swap! db assoc-in [:sign-in-with-github attempt-id] {:initialised-at (time/now) :client-id client-id})
    (?reply-fn [id {:attempt-id attempt-id :client-id client-id :scope scope}])))


(defmethod handler :general/init
  [{:keys [config-opts db]} {:keys [uid ?reply-fn] [id ?data] :event}]
  (let [user (some-> (get-in @db [:users uid]) (select-keys [:name :email :login :avatar-url]))]
    (?reply-fn [id {:me user}])))


(defmethod handler :chsk/ws-ping
  [φ event-message])


(defmethod handler :chsk/uidport-open
  [φ event-message])


(defmethod handler :chsk/uidport-close
  [φ event-message])


(defmethod handler :default
  [φ {:keys [id]}]
  (log/debug "unhandled event-message:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; top level handling


(defn make-handler
  "Returns a function that receives a message and handles it appropriately via multimethods"
  [φ]
  (fn [{:keys [id client-id uid] :as event-message}]
    (log/debugf "received %s from client %s with uid %s" id client-id uid)
    (handler φ event-message)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts db]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [chsk-conn (sente/make-channel-socket! sente-web-server-adapter (get-in config-opts [:comms :chsk-opts]))
          stop-chsk-router! (sente/start-chsk-router! (:ch-recv chsk-conn) (make-handler {:config-opts config-opts :db db}))]
      (assoc component
             :chsk-conn chsk-conn
             :stop-chsk-router! stop-chsk-router!)))

  (stop [component]
    (when-let [stop-chsk-router! (:stop-chsk-router! component)]
      (log/info "stopping comms")
      (stop-chsk-router!))
    component))
