(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.history Html5History EventType]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; comms setup


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [emit-event!]

  (fn [{:keys [event id ?data send-fn]}]
    (log/info "received message from server:" id)
    (cond
      (= id :chsk/state) (do (log/info "chsk status change")
                             (emit-event! [:chsk-status-update {:Δ (fn [core] (assoc core :chsk-status (if (:open? ?data) :open :closed)))}]))
      (= id :chsk/handshake) (log/info "chsk handshake"))))


(defn make-send!
  "Returns a function that takes a message to send, an event is emitted
   when the message is dispatched, and another when the message reply is received."
  [send-fn emit-event!]

  (fn [[id _ :as message] {:keys [timeout]}]
    (log/debug "dispatching message to server:" id)
    (emit-event! [:message-sent {:Δ (fn [core] (assoc-in core [:message-status id] :sent))}])

    (send-fn
      message
      (or timeout 10000)
      (fn [reply]
        (if (sente/cb-success? reply)
          (let [[id ?payload] reply]
            (log/debug "received a reply message from server:" id)
            (emit-event! [:message-reply-received {:Δ (fn [core] (-> core
                                                                    (assoc-in [:message-status id] :reply-received)
                                                                    (assoc-in [:message-reply id] ?payload)))}]))
          (do
            (log/warn "message failed with:" reply)
            (emit-event! [:message-failed {:Δ (fn [core] (-> core
                                                            (assoc-in [:message-status id] :failed)
                                                            (assoc-in [:message-reply id] reply)))}])))))))


(defn make-post!
  "Returns a function that makes an ajax post to the server. An event is emitted
   when the request is made, and another when the response is received."
  [chsk chsk-state emit-event!]

  (fn [[path params] {:keys [timeout]}]
    (log/debug "dispatching post to server:" path)
    (emit-event! [:post-sent {:Δ (fn [core] (assoc-in core [:post-status path] :sent))}])

    (sente/ajax-lite
     path
     {:method :post
      :timeout-ms (or timeout 10000)
      :params (merge params (select-keys @chsk-state [:csrf-token]))}
     (fn [response]
       (if (:success? response)
         (do
           (log/debug "received a post response from server:" path)
           (emit-event! [:post-response-received {:Δ (fn [core] (-> core
                                                                   (assoc-in [:post-status path] :response-received)
                                                                   (assoc-in [:post-response path] (:?content response))))}])
           ;; we reconnect the websocket connection here to pick up any changes
           ;; in the session that may have come about with the post request
           (sente/chsk-reconnect! chsk))
         (do
           (log/warn "post failed with:" response)
           (emit-event! [:post-failed {:Δ (fn [core] (-> core
                                                        (assoc-in [:post-status path] :failed)
                                                        (assoc-in [:post-response path] response)))}])))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [emit-event! (:emit-event! bus)
          {:keys [chsk ch-recv send-fn] chsk-state :state} (sente/make-channel-socket! "/chsk" (get-in config-opts [:side-effector :chsk-opts]))]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-receive emit-event!))

      (assoc component
             :send! (make-send! send-fn emit-event!)
             :post! (make-post! chsk chsk-state emit-event!))))

  (stop [component] component))

