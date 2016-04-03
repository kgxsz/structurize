(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.history Html5History EventType]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method handling


(defmulti handler (fn [_ event-message] (:id event-message)))


(defmethod handler :chsk/state
  [emit-event! {:keys [event id ?data send-fn] :as event-message}]
  (emit-event! [:comms/chsk-status-update {:Δ (fn [c] (assoc-in c [:comms :chsk-status] (if (:open? ?data) :open :closed)))}]))


(defmethod handler :chsk/handshake
  [])

(defmethod handler :default
  [emit-event! {:keys [event id ?data send-fn] :as event-message}]
  (log/debug "unhandled event-message:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; comms setup


(defn make-handler
  "Returns a function that receives a message and handles it appropriately via multimethods"
  [emit-event!]

  (fn [{:keys [event id ?data send-fn] :as event-message}]
    (log/debug "received message from server:" id)
    (handler emit-event! event-message)))


(defn make-send

  "Returns a function that takes a message to send, an event is emitted
   when the message is dispatched, and another when the message reply is received.

   The returned function expects:

   message - the sente message, in the form of a vector, with id
   timeout - in milliseconds"

  [send-fn emit-event!]

  (fn [[id _ :as message] {:keys [timeout]}]
    (log/debug "dispatching message to server:" id)
    (emit-event! [:comms/message-sent {:Δ (fn [c] (assoc-in c [:comms :message id :status] :sent))}])

    (send-fn
      message
      (or timeout 10000)
      (fn [reply]
        (if (sente/cb-success? reply)
          (let [[id ?payload] reply]
            (log/debug "received a reply message from server:" id)
            (emit-event! [:comms/message-reply-received {:Δ (fn [c] (-> c
                                                                 (assoc-in [:comms :message id :status] :reply-received)
                                                                 (assoc-in [:comms :message id :reply] ?payload)))}]))
          (do
            (log/warn "message failed with:" reply)
            (emit-event! [:comms/message-failed {:Δ (fn [c] (-> c
                                                         (assoc-in [:comms :message id :status] :failed)
                                                         (assoc-in [:comms :message id :reply] reply)))}])))))))


(defn make-post

  "Returns a function that makes an ajax post to the server. An event is emitted
   when the request is made, and another when the response is received, one subtelty
   worth mentioning is that posting is only used to perform session mutating actions,
   as such, we must remove the general/init message to trigger a re-fetch and then
   we need to reconnect the chsk upon the successful receipt of a post response.

   The returned function expects:

   path - path to post to
   params - map of params to post
   timeout - in milliseconds"

  [chsk chsk-state emit-event!]

  (fn [[path params] {:keys [timeout]}]
    (log/debug "dispatching post to server:" path)
    (emit-event! [:comms/post-sent {:Δ (fn [c] (assoc-in c [:comms :post path :status] :sent))}])

    (sente/ajax-lite
     path
     {:method :post
      :timeout-ms (or timeout 10000)
      :params (merge params (select-keys @chsk-state [:csrf-token]))}
     (fn [response]
       (if (:success? response)
         (do
           (log/debug "received a post response from server:" path)
           (emit-event! [:comms/post-response-received {:Δ (fn [c] (-> c
                                                                (assoc-in [:comms :post path :status] :response-received)
                                                                (assoc-in [:comms :post path :response] (:?content response))
                                                                (assoc-in [:comms :chsk-status] :closed)
                                                                (assoc-in [:comms :message :general/init] nil)))}])
           ;; we reconnect the websocket connection here to pick up any changes
           ;; in the session that may have come about with the post request
           (sente/chsk-reconnect! chsk))
         (do
           (log/warn "post failed with:" response)
           (emit-event! [:comms/post-failed {:Δ (fn [c] (-> c
                                                     (assoc-in [:comms :post path :status] :failed)
                                                     (assoc-in [:comms :post path :response] response)))}])))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts machine]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [emit-event! (:emit-event! machine)
          chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [chsk ch-recv send-fn] chsk-state :state} (sente/make-channel-socket! "/chsk" chsk-opts)]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-handler emit-event!))

      (assoc component
             :send! (make-send send-fn emit-event!)
             :post! (make-post chsk chsk-state emit-event!))))

  (stop [component] component))

