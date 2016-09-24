(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [structurize.system.state :refer [read write!]]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:import [goog.history Html5History EventType]))

;; exposed functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send!
  "Takes a message to send, an mutation is emitted
   when the message is dispatched, and another when the message reply is received.

   Params:
   message - the sente message, in the form of a vector, with id
   timeout - in milliseconds"
  [{:keys [chsk-send] :as φ} id params {:keys [timeout on-success on-failure]}]

  (let [φ (assoc φ :context {:comms? true})]

    (log/debug "dispatching message to server:" id)
    (write! φ :comms/message-sent
            (fn [x]
              (assoc-in x [:comms :message id] {:status :sent})))

    (chsk-send
     [id params]
     (or timeout 10000)
     (fn [reply]
       (if (sente/cb-success? reply)
         (let [[id _] reply]
           (log/debug "received a message reply from server:" id)
           (write! φ :comms/message-reply-received
                   (fn [x]
                     (-> x
                         (assoc-in [:comms :message id :status] :reply-received)
                         (assoc-in [:comms :message id :reply] (second reply)))))
           (when on-success (on-success reply)))
         (do
           (log/warn "message failed with:" reply)
           (write! φ :comms/message-failed
                   (fn [x]
                     (-> x
                         (assoc-in [:comms :message id :status] :failed)
                         (assoc-in [:comms :message id :reply] reply))))
           (when on-failure (on-failure reply))))))))


(defn post!
  "Makes an ajax post to the server. A mutation is emitted
   when the request is made, and another when the response is received, one subtelty
   worth mentioning is that posting is only used to perform session mutating actions,
   as such, we need to reconnect the chsk upon the successful receipt of a post response.

   Params:
   path - path to post to
   params - map of params to post
   timeout - in milliseconds"
  [{:keys [chsk chsk-state] :as φ} path params {:keys [timeout on-success on-failure]}]

  (let [φ (assoc φ :context {:comms? true})]
    (log/debug "dispatching post to server:" path)

    (write! φ :comms/post-sent
            (fn [x]
              (assoc-in x [:comms :post path] {:status :sent})))

    (sente/ajax-lite
     path
     {:method :post
      :timeout-ms (or timeout 10000)
      :params (merge params (select-keys @chsk-state [:csrf-token]))}
     (fn [response]
       (if (:success? response)
         (do
           (log/debug "received a post response from server:" path)
           (write! φ :comms/post-response-received
                   (fn [x]
                     (-> x
                         (assoc-in [:comms :post path :status] :response-received)
                         (assoc-in [:comms :post path :response] (:?content response))
                         (assoc :app-status :uninitialised))))
           (when on-success (on-success response))

           ;; we reconnect the websocket connection here to pick up any changes
           ;; in the session that may have come about with the post request
           (sente/chsk-reconnect! chsk))
         (do
           (log/warn "post failed with:" response)
           (write! φ :comms/post-failed
                   (fn [x]
                     (-> x
                         (assoc-in [:comms :post path :status] :failed)
                         (assoc-in [:comms :post path :response] response))))
           (when on-failure (on-failure response))))))))


;; backend to frontend message handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-received-message (fn [_ event-message] (:id event-message)))


(defmethod process-received-message :chsk/state
  [{:keys [config-opts] :as Φ} {:keys [event id] [old-state new-state] :?data :as event-message}]
  (let [chsk-status (if (:open? new-state) :open :closed)
        app-uninitialised? (= :uninitialised
                              (read Φ l/view-single
                                    (in [:app-status])))]
    (write! Φ :comms/chsk-status-update
            (fn [x]
              (assoc-in x [:comms :chsk-status] chsk-status)))

    (when (and (= :open chsk-status) app-uninitialised?)
      (write! Φ :general/app-initialising
              (fn [x] (assoc x :app-status :initialising)))
      (send! Φ :general/initialise-app
             {}
             {:on-success (fn [[_ {:keys [me]}]]
                            (write! Φ :general/app-initialised
                                    (fn [x]
                                      (cond-> x
                                        me (assoc-in [:auth :me] me)
                                        true (assoc :app-status :initialised)))))
              :on-failure (fn [reply]
                            (write! Φ :general/app-initialisation-failed
                                    (fn [x]
                                      (assoc x :app-status :initialisation-failed))))}))))


(defmethod process-received-message :chsk/handshake
  [])


(defmethod process-received-message :default
  [_ {:keys [id]}]
  (log/debug "no handler for received message:" id))


;; helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-receive-message
  "Returns a function that receives a message and processes it appropriately via multimethods"
  [{:keys [config-opts] :as φ}]

  (fn [{:keys [event id data] :as event-message}]
    (log/debug "received message from server:" id)
    (process-received-message φ event-message)))


;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Comms [config-opts state]
  component/Lifecycle

  (start [component]
    (log/info "initialising comms")
    (let [chsk-opts (get-in config-opts [:comms :chsk-opts])
          {:keys [chsk ch-recv] chsk-send :send-fn chsk-state :state} (sente/make-channel-socket! "/chsk" chsk-opts)
          φ {:context {:comms? true}
             :config-opts config-opts
             :!state (:!state state)
             :chsk chsk
             :chsk-state chsk-state
             :chsk-send chsk-send}]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-receive-message φ))

      (assoc component
             :chsk chsk
             :chsk-state chsk-state
             :chsk-send chsk-send)))

  (stop [component] component))
