(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [structurize.system.utils :refer [side-effect!]]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:import [goog.history Html5History EventType]))


;; backend to frontend message handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-received-message (fn [_ event-message] (:id event-message)))


(defmethod process-received-message :chsk/state
  [{:keys [config-opts] :as Φ} {:keys [event id ?data] :as event-message}]
  (side-effect! Φ :comms/chsk-status-update
                {:status (if (:open? ?data) :open :closed)}))


(defmethod process-received-message :chsk/handshake
  [])


(defmethod process-received-message :default
  [_ {:keys [id]}]
  (log/debug "no handler for received message:" id))


;; helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-receive-message
  "Returns a function that receives a message and processes it appropriately via multimethods"
  [{:keys [config-opts] :as φ}]

  (fn [{:keys [event id ?data] :as event-message}]
    (log/debug "received message from server:" id)
    (process-received-message φ event-message)))


;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Comms [config-opts state side-effector]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [chsk ch-recv] chsk-send :send-fn chsk-state :state} (sente/make-channel-socket! "/chsk" chsk-opts)
          φ {:context {:comms? true}
             :config-opts config-opts
             :!state (:!state state)
             :<side-effects (:<side-effects side-effector)
             :chsk chsk
             :chsk-state chsk-state
             :chsk-send chsk-send}]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-receive-message φ))

      (assoc component
             :chsk chsk
             :chsk-state chsk-state
             :chsk-send chsk-send)))

  (stop [component] component))

