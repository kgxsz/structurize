(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [structurize.system.side-effect-bus :refer [side-effect!]]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.history Html5History EventType]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method message handling


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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; comms setup


(defn make-receive-message
  "Returns a function that receives a message and processes it appropriately via multimethods"
  [{:keys [config-opts] :as Φ}]

  (fn [{:keys [event id ?data] :as event-message}]
    (log/debug "received message from server:" id)
    (process-received-message Φ event-message)))


(defn send!
  "Takes a message to send, an mutation is emitted
   when the message is dispatched, and another when the message reply is received.

   Params:
   message - the sente message, in the form of a vector, with id
   timeout - in milliseconds"
  [{:keys [chsk-send] :as Φ} id params {:keys [timeout on-success on-failure]}]

  (log/debug "dispatching message to server:" id)
  (side-effect! Φ :comms/message-sent
                {:message-id id})

  (chsk-send
   [id params]
   (or timeout 10000)
   (fn [reply]
     (if (sente/cb-success? reply)
       (let [[id _] reply]
         (log/debug "received a message reply from server:" id)
         (side-effect! Φ :comms/message-reply-received
                       {:message-id id :reply reply :on-success on-success}))
       (do
         (log/warn "message failed with:" reply)
         (side-effect! Φ :comms/message-failed
                       {:message-id id :reply reply :on-failure on-failure}))))))


(defn post!
  "Makes an ajax post to the server. A mutation is emitted
   when the request is made, and another when the response is received, one subtelty
   worth mentioning is that posting is only used to perform session mutating actions,
   as such, we need to reconnect the chsk upon the successful receipt of a post response.

   Params:
   path - path to post to
   params - map of params to post
   timeout - in milliseconds"
  [{:keys [chsk chsk-state] :as Φ} path params {:keys [timeout on-success on-failure]}]

  (log/debug "dispatching post to server:" path)

  (side-effect! Φ :comms/post-sent
                {:path path})

  (sente/ajax-lite
   path
   {:method :post
    :timeout-ms (or timeout 10000)
    :params (merge params (select-keys @chsk-state [:csrf-token]))}
   (fn [response]
     (if (:success? response)
       (do
         (log/debug "received a post response from server:" path)
         (side-effect! Φ :comms/post-response-received
                       {:path path :response response :on-success on-success})

         ;; we reconnect the websocket connection here to pick up any changes
         ;; in the session that may have come about with the post request
         (sente/chsk-reconnect! chsk))
       (do
         (log/warn "post failed with:" response)
         (side-effect! Φ :comms/post-failed
                       {:path path :response response :on-failure on-failure}))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts state side-effect-bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [chsk ch-recv] chsk-send :send-fn chsk-state :state} (sente/make-channel-socket! "/chsk" chsk-opts)]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-receive-message {:config-opts config-opts
                                                               :!state (:!state state)
                                                               :<side-effects (:<side-effects side-effect-bus)
                                                               :chsk chsk
                                                               :chsk-state chsk-state
                                                               :chsk-send chsk-send}))

      (assoc component
             :chsk chsk
             :chsk-state chsk-state
             :chsk-send chsk-send)))

  (stop [component] component))

