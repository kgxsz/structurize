(ns structurize.system.comms
  (:require [com.stuartsierra.component :as component]
            [structurize.system.side-effector :refer [process-side-effect side-effect!]]
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
    (side-effect! φ :comms/message-sent
                  {:message-id id})

    (chsk-send
     [id params]
     (or timeout 10000)
     (fn [reply]
       (if (sente/cb-success? reply)
         (let [[id _] reply]
           (log/debug "received a message reply from server:" id)
           (side-effect! φ :comms/message-reply-received
                         {:message-id id :reply reply :on-success on-success}))
         (do
           (log/warn "message failed with:" reply)
           (side-effect! φ :comms/message-failed
                         {:message-id id :reply reply :on-failure on-failure})))))))


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

    (side-effect! φ :comms/post-sent
                  {:path path})

    (sente/ajax-lite
     path
     {:method :post
      :timeout-ms (or timeout 10000)
      :params (merge params (select-keys @chsk-state [:csrf-token]))}
     (fn [response]
       (if (:success? response)
         (do
           (log/debug "received a post response from server:" path)
           (side-effect! φ :comms/post-response-received
                         {:path path :response response :on-success on-success})

           ;; we reconnect the websocket connection here to pick up any changes
           ;; in the session that may have come about with the post request
           (sente/chsk-reconnect! chsk))
         (do
           (log/warn "post failed with:" response)
           (side-effect! φ :comms/post-failed
                         {:path path :response response :on-failure on-failure})))))))


;; backend to frontend message handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-received-message (fn [_ event-message] (:id event-message)))


(defmethod process-received-message :chsk/state
  [{:keys [config-opts] :as Φ} {:keys [event id] [old-state new-state] :?data :as event-message}]
  (side-effect! Φ :comms/chsk-status-update
                {:status (if (:open? new-state) :open :closed)}))


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


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :comms/chsk-status-update
  [Φ id {chsk-status :status :as props}]
  (let [app-uninitialised? (= :uninitialised
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


(defmethod process-side-effect :comms/message-sent
  [Φ id {:keys [message-id] :as props}]
  (write! Φ :comms/message-sent
          (fn [x]
            (assoc-in x [:comms :message message-id] {:status :sent}))))


(defmethod process-side-effect :comms/message-reply-received
  [Φ id {:keys [message-id reply on-success] :as props}]
  (write! Φ :comms/message-reply-received
          (fn [x]
            (-> x
                (assoc-in [:comms :message message-id :status] :reply-received)
                (assoc-in [:comms :message message-id :reply] (second reply)))))
  (when on-success (on-success reply)))


(defmethod process-side-effect :comms/message-failed
  [Φ id {:keys [message-id reply on-failure] :as props}]
  (write! Φ :comms/message-failed
          (fn [x]
            (-> x
                (assoc-in [:comms :message message-id :status] :failed)
                (assoc-in [:comms :message message-id :reply] reply))))
  (when on-failure (on-failure reply)))


(defmethod process-side-effect :comms/post-sent
  [Φ id {:keys [path] :as props}]
  (write! Φ :comms/post-sent
          (fn [x]
            (assoc-in x [:comms :post path] {:status :sent}))))



(defmethod process-side-effect :comms/post-response-received
  [Φ id {:keys [path response on-success] :as props}]
  (write! Φ :comms/post-response-received
          (fn [x]
            (-> x
                (assoc-in [:comms :post path :status] :response-received)
                (assoc-in [:comms :post path :response] (:?content response))
                (assoc :app-status :uninitialised))))
  (when on-success (on-success response)))


(defmethod process-side-effect :comms/post-failed
  [Φ id {:keys [path response on-failure] :as props}]
  (write! Φ :comms/post-failed
          (fn [x]
            (-> x
                (assoc-in [:comms :post path :status] :failed)
                (assoc-in [:comms :post path :response] response))))
  (when on-failure (on-failure response)))
