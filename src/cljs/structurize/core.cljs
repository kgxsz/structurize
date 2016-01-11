(ns structurize.core
  (:require [reagent.core :as r]
            [taoensso.sente :as sente]
            [cljs.core.async :as a]
            [taoensso.timbre :as log]))



(defn root-view []
  [:div
   [:h1 "Front end ready!"]
   [:h3 "more to come.."]])

(defn render-root! []
  (r/render [root-view] (js/document.getElementById "root")))

(defn send-hello [send-fn]
  (let [hello-ev [:hello/world "HIYA"]]
    (log/info "Sending message to server:" hello-ev)
    (send-fn
     hello-ev
     5000
     (fn [reply]
       (log/info "Got a reply!:" reply)))))

(defn receive-event! [{:keys [event send-fn]}]
  (let [[id ?payload] event]
    (log/info "Received an event!" id)
    (when (and (= id :chsk/state)
               (:first-open? ?payload))
      (log/info "Communications established!")
      (send-hello send-fn))))


(defn main []
  (let [{:keys [ch-recv send-fn]} (sente/make-channel-socket! "/chsk" {:type :auto})]
    (render-root!)
    (sente/start-chsk-router! ch-recv receive-event!)))
