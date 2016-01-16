(ns structurize.system
  (:require [structurize.env :refer [env]]
            [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;; Events message handling ;;;;;;;;;;;;;;;;;;;;;;;;;


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



;;;;;;;;;;;;;;;;;;;;;;;;; Components ;;;;;;;;;;;;;;;;;;;;;;;;;


(defrecord ChskConn [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Initialising channel socket connection")
    (let [chsk-conn (sente/make-channel-socket! "/chsk" (:chsk-opts config-opts))]
      (sente/start-chsk-router! (:ch-recv chsk-conn) event-msg-handler)
      (assoc component :chsk-conn chsk-conn)))

  (stop [component] component))


(defrecord AppState []
  component/Lifecycle

  (start [component]
    (log/info "Initialising app-state")
    (let [app-state {:fruit "banana"}]
      (assoc component :app-state (r/atom app-state))))

  (stop [component] component))



;;;;;;;;;;;;;;;;;;;;;;;;; System ;;;;;;;;;;;;;;;;;;;;;;;;;


(defonce system-config-opts
  {:chsk-conn-config-opts {:chsk-opts {:type :auto}}})


(defn make-system [config-opts]
  (component/system-map
   :app-state (map->AppState {})
   :chsk-conn (map->ChskConn {:config-opts (:chsk-conn-config-opts config-opts)})))


(defonce system (component/start (make-system system-config-opts)))
