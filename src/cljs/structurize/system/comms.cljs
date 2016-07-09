(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.history Html5History EventType]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method message handling


(defmulti process-received-message (fn [_ _ event-message] (:id event-message)))


(defmethod process-received-message :chsk/state
  [config-opts side-effect! {:keys [event id ?data send-fn] :as event-message}]
  (side-effect! [:comms/chsk-status-update {:status (if (:open? ?data) :open :closed)}]))


(defmethod process-received-message :chsk/handshake
  [])


(defmethod process-received-message :default
  [_ _ {:keys [id]}]
  (log/debug "no handler for received message:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; comms setup


(defn make-receive-message
  "Returns a function that receives a message and processes it appropriately via multimethods"
  [config-opts side-effect!]

  (fn [{:keys [event id ?data send-fn] :as event-message}]
    (log/debug "received message from server:" id)
    (process-received-message config-opts side-effect! event-message)))


(defn make-send

  "Returns a function that takes a message to send, an mutation is emitted
   when the message is dispatched, and another when the message reply is received.

   The returned function expects:

   message - the sente message, in the form of a vector, with id
   timeout - in milliseconds"

  [send-fn side-effect!]

  (fn [[id _ :as message] {:keys [timeout on-success on-failure]}]
    (log/debug "dispatching message to server:" id)
    (side-effect! [:comms/message-sent {:message-id id}])

    (send-fn
      message
      (or timeout 10000)
      (fn [reply]
        (if (sente/cb-success? reply)
          (let [[id _] reply]
            (log/debug "received a message reply from server:" id)
            (side-effect! [:comms/message-reply-received {:message-id id :reply reply :on-success on-success}]))
          (do
            (log/warn "message failed with:" reply)
            (side-effect! [:comms/message-failed {:message-id id :reply reply :on-failure on-failure}])))))))


(defn make-post

  "Returns a function that makes an ajax post to the server. A mutation is emitted
   when the request is made, and another when the response is received, one subtelty
   worth mentioning is that posting is only used to perform session mutating actions,
   as such, we need to reconnect the chsk upon the successful receipt of a post response.

   The returned function expects:

   path - path to post to
   params - map of params to post
   timeout - in milliseconds"

  [chsk chsk-state side-effect!]

  (fn [[path params] {:keys [timeout on-success on-failure]}]
    (log/debug "dispatching post to server:" path)

    (side-effect! [:comms/post-sent {:path path}])

    (sente/ajax-lite
     path
     {:method :post
      :timeout-ms (or timeout 10000)
      :params (merge params (select-keys @chsk-state [:csrf-token]))}
     (fn [response]
       (if (:success? response)
         (do
           (log/debug "received a post response from server:" path)
           (side-effect! [:comms/post-response-received {:path path :response response :on-success on-success}])

           ;; we reconnect the websocket connection here to pick up any changes
           ;; in the session that may have come about with the post request
           (sente/chsk-reconnect! chsk))
         (do
           (log/warn "post failed with:" response)
           (side-effect! [:comms/post-failed {:path path :response response :on-failure on-failure}])))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts side-effect-bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [side-effect! (:side-effect! side-effect-bus)
          chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [chsk ch-recv send-fn] chsk-state :state} (sente/make-channel-socket! "/chsk" chsk-opts)]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-receive-message config-opts side-effect!))

      (assoc component
             :send! (make-send send-fn side-effect!)
             :post! (make-post chsk chsk-state side-effect!))))

  (stop [component] component))

