(ns structurize.event-msg-handler
  (:require [taoensso.timbre :as log]))


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

