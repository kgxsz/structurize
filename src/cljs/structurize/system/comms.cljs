(ns structurize.system.comms
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [{:keys [emit-event!]}]

  (fn [{:keys [event id ?data]}]
    (log/info "Received message:" id)
    #_(go (a/>! <event [:comms-event {:id id :?data ?data}]))
    (when (and (= id :chsk/state) (= (:first-open? ?data)))
         (log/info "Communications established"))))


(defn make-send!
  "Returns a function that takes a message to send, a timeout, and a callback function to run on the reply."
  [send-fn {:keys [emit-event!]}]

  (fn [{[id _ :as message] :message, :keys [timeout]}]
    (log/debug "Sending message to server:" id)
    (emit-event! [:message-sent {:Δ (fn [core] (assoc-in core [:message-status id] :sent))}])

    (send-fn
      message
      (or timeout 10000)
      (fn [reply]
        (if (sente/cb-success? reply)
          (let [[id ?payload] reply]
            (log/debug "Received a reply message from server:" reply)
            (emit-event! [:message-received {:Δ (fn [core]
                                                  (-> core
                                                      (assoc-in [:message-status id] :received)
                                                      (assoc-in [:message-reply id] ?payload)))}]))
          (do
            (log/error "Comms failed with:" reply)
            (emit-event! [:message-failed {:Δ (fn [core] (assoc-in core [:message-status id] :failed))}])))))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord Comms [config-opts bus]
  component/Lifecycle

  (start [component]
    (log/info "Initialising comms")
    (let [chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [ch-recv send-fn]} (sente/make-channel-socket! "/chsk" chsk-opts)]
      (sente/start-chsk-router! ch-recv (make-receive bus))
      (assoc component :send! (make-send! send-fn bus))))

  (stop [component] component))
