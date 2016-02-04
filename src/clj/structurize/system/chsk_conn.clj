(ns structurize.system.chsk-conn
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :as log]
            [clojure.core.async :refer [go <! timeout]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; event message handling


(defn event-msg-handler [{:keys [event send-fn ring-req ?reply-fn]}]
  (let [[id ?payload] event]
    (log/info "Received event!" id)
    (when (and (= :auth/login-with-github id) ?reply-fn)
      (go (<! (timeout 2000))
          (?reply-fn [:auth/login-with-github {:attempt-id 123 :client-id 456}])))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord ChskConn [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Initialising chsk-conn")
    (let [chsk-conn (sente/make-channel-socket! sente-web-server-adapter {})
          stop-chsk-router! (sente/start-chsk-router! (:ch-recv chsk-conn) event-msg-handler)]
      (assoc component
             :ajax-get-or-ws-handshake-fn (:ajax-get-or-ws-handshake-fn chsk-conn)
             :ajax-post-fn (:ajax-post-fn chsk-conn)
             :stop-chsk-router! stop-chsk-router!)))

  (stop [component]
    (when-let [stop-chsk-router! (:stop-chsk-router! component)]
      (log/info "Stopping chsk-conn")
      (stop-chsk-router!))
    component))
