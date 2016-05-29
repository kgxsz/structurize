(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.history Html5History EventType]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method message handling


(defmulti process-received-message (fn [_ _ event-message] (:id event-message)))


(defmethod process-received-message :chsk/state
  [config-opts emit-side-effect! {:keys [event id ?data send-fn] :as event-message}]
  (emit-side-effect! [:comms/chsk-status-update {:status (if (:open? ?data) :open :closed)}]))


(defmethod process-received-message :chsk/handshake
  [])

(defmethod process-received-message :default
  [_ _ {:keys [id]}]
  (log/debug "failed to process received message:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; comms setup


(defn make-receive-message
  "Returns a function that receives a message and processes it appropriately via multimethods"
  [config-opts emit-side-effect!]

  (fn [{:keys [event id ?data send-fn] :as event-message}]
    (log/debug "received message from server:" id)
    (process-received-message config-opts emit-side-effect! event-message)))


(defn make-send

  "Returns a function that takes a message to send, an mutation is emitted
   when the message is dispatched, and another when the message reply is received.

   The returned function expects:

   message - the sente message, in the form of a vector, with id
   timeout - in milliseconds"

  [send-fn emit-side-effect!]

  (fn [[id _ :as message] {:keys [timeout]}]
    (log/debug "dispatching message to server:" id)
    (emit-side-effect! [:comms/message-sent {:message-id id}])

    (send-fn
      message
      (or timeout 10000)
      (fn [reply]
        (if (sente/cb-success? reply)
          (let [[id ?payload] reply]
            (log/debug "received a reply message from server:" id)
            (emit-side-effect! [:comms/message-reply-received {:message-id id :payload ?payload}]))
          (do
            (log/warn "message failed with:" reply)
            (emit-side-effect! [:comms/message-failed {:message-id id :reply reply}])))))))


(defn make-post

  "Returns a function that makes an ajax post to the server. A mutation is emitted
   when the request is made, and another when the response is received, one subtelty
   worth mentioning is that posting is only used to perform session mutating actions,
   as such, we must remove the general/init message to trigger a re-fetch and then
   we need to reconnect the chsk upon the successful receipt of a post response.

   The returned function expects:

   path - path to post to
   params - map of params to post
   timeout - in milliseconds"

  [chsk chsk-state emit-side-effect!]

  (fn [[path params] {:keys [timeout]}]
    (log/debug "dispatching post to server:" path)

    (emit-side-effect! [:comms/post-sent {:path path}])

    (sente/ajax-lite
     path
     {:method :post
      :timeout-ms (or timeout 10000)
      :params (merge params (select-keys @chsk-state [:csrf-token]))}
     (fn [response]
       (if (:success? response)
         (do
           (log/debug "received a post response from server:" path)
           (emit-side-effect! [:comms/post-response-received {:path path :response response}])

           ;; we reconnect the websocket connection here to pick up any changes
           ;; in the session that may have come about with the post request
           (sente/chsk-reconnect! chsk))
         (do
           (log/warn "post failed with:" response)
           (emit-side-effect! [:comms/post-failed {:path path :response response}])))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts side-effect-bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [emit-mutation! (:emit-mutation! state)
          emit-side-effect! (:emit-side-effect! side-effect-bus)
          chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [chsk ch-recv send-fn] chsk-state :state} (sente/make-channel-socket! "/chsk" chsk-opts)]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-receive-message config-opts emit-side-effect!))

      (assoc component
             :send! (make-send send-fn emit-side-effect!)
             :post! (make-post chsk chsk-state emit-side-effect!))))

  (stop [component] component))

