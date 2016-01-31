(ns structurize.system.chsk-conn
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log]))


(defn send-hello
  [send-fn]
  (let [hello-ev [:hello/world "HIYA"]]
    (log/info "Sending message to server:" hello-ev)
    (send-fn
     hello-ev
     5000
     (fn [reply]
       (log/info "Got a reply!:" reply)))))


(defn event-msg-handler
  [{:keys [event id ?data]}]
  (log/info "Received event message:" id)
  (when (and (= id :chsk/state) (= (:first-open? ?data)))
    (log/info "Communications established")))


(defrecord ChskConn [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Initialising chsk-conn")
    (let [chsk-opts (get-in config-opts [:chsk-conn :chsk-opts])
          {:keys [ch-recv chsk-send!]} (sente/make-channel-socket! "/chsk" chsk-opts)]
      (sente/start-chsk-router! ch-recv event-msg-handler)
      (assoc component
             :ch-recv ch-recv
             :chsk-send! chsk-send!)))

  (stop [component] component))
