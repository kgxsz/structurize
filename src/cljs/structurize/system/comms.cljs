(ns structurize.system.comms
  (:require [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn make-receive [{:keys [emit-event!]}]
  (fn [{:keys [event id ?data]}]
    (log/info "Received message from server:" id)
    #_(go (a/>! <event [:comms-event {:id id :?data ?data}]))
    (when (and (= id :chsk/state) (= (:first-open? ?data)))
         (log/info "Communications established"))))


(defn make-send! [send-fn {:keys [emit-event!]} {:keys [!global]}]
  (fn [{[id _ :as message] :message, :keys [timeout]}]
    (log/debug "Sending message to server:" id)
    (emit-event! [:message-status {:Δ (fn [state] (update-in state [:message-status id] (constantly :sent)))}])
    (send-fn
     message
     (or timeout 5000)
     (fn [[id ?payload :as reply]]
       (if (sente/cb-success? reply)
         (do (log/debug "Received a reply message from server:" reply)
             (emit-event! [:message-reply {:Δ (fn [state]
                                                (-> state
                                                    (update-in [:message-status id] (constantly :received))
                                                    (update-in [:message-reply id] (constantly ?payload))))}]))
         (log/debug "Comms failed with:" reply))))))


(defrecord Comms [config-opts bus state]
  component/Lifecycle

  (start [component]
    (log/info "Initialising comms")
    (let [chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [ch-recv send-fn]} (sente/make-channel-socket! "/chsk" chsk-opts)]
      (sente/start-chsk-router! ch-recv (make-receive bus))
      (assoc component :send! (make-send! send-fn bus state))))

  (stop [component] component))
