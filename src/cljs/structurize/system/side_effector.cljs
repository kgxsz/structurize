(ns structurize.system.side-effector
  (:require [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [cemerick.url :refer [url]]
            [cljs.core.async :as a]
            [com.stuartsierra.component :as component]
            [goog.events :as events]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.core :refer [defroute]])
  (:import [goog.history Html5History EventType]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; history setup


(defn make-navigation-handler
  "Returns a function that handles google navigation
   events and emits the relevant event."
  [emit-event!]

  (fn [g-event]
    (let [url (url js/window.location.href)
          host (str (:protocol url) "://" (:host url) (when (pos? (:port url)) (str ":" (:port url))))
          location (merge (select-keys url [:path :query])
                          (b/match-route routes (:path url))
                          {:host host})]
      (log/debug "received navigation from browser:" (:path location))
      (when-not (.-isNavigation g-event) (js/window.scrollTo 0 0))
      (emit-event! [:location-change {:Δ (fn [core] (assoc core :location location))}]))))


(defn make-history []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol "//" js/window.location.host))
    (.setUseFragment false)))


(defn listen-for-navigation [history handler]
  (doto history
    (goog.events/listen EventType.NAVIGATE #(handler %))
    (.setEnabled true)))


(defn make-change-history!
  "Returns a function that receives a token and changes the history.
   If replace? is true, the history will be replaced rather than put
   in the stack, such that the change will not be included in forward
   and back navigation. If leave? is true, then we leave the app entirely."
  [emit-event!]
  (let [history (make-history)
        navigation-handler (make-navigation-handler emit-event!)]
    (listen-for-navigation history navigation-handler)
    (fn change-history!
      ([token] (change-history! token {}))
      ([token {:keys [replace? leave?]}]
       (log/debug "dispatching navigation to browser:" token)
       (cond
         leave? (set! js/window.location token)
         replace? (.replaceToken history token)
         :else (.setToken history token))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; comms setup


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [emit-event!]

  (fn [{:keys [event id ?data]}]
    (log/info "received message from server:" id)
    #_(go (a/>! <event [:comms-event {:id id :?data ?data}]))
    (when (and (= id :chsk/state) (= (:first-open? ?data)))
         (log/info "comms established"))))


(defn make-send!
  "Returns a function that takes a message to send, an event is emitted
   when the message is dispatched, and another when the message reply is received."
  [send-fn emit-event!]

  (fn [{[id _ :as message] :message, :keys [timeout]}]
    (log/debug "dispatching message to server:" id)
    (emit-event! [:message-sent {:Δ (fn [core] (assoc-in core [:message-status id] :sent))}])

    (send-fn
      message
      (or timeout 10000)
      (fn [reply]
        (if (sente/cb-success? reply)
          (let [[id ?payload] reply]
            (log/debug "received a reply message from server:" id)
            (emit-event! [:message-received {:Δ (fn [core]
                                                  (-> core
                                                      (assoc-in [:message-status id] :received)
                                                      (assoc-in [:message-reply id] ?payload)))}]))
          (do
            (log/error "comms failed with:" reply)
            (emit-event! [:message-failed {:Δ (fn [core] (assoc-in core [:message-status id] :failed))}])))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; event emitter setup


(defn make-emit-event!
  "Returns a function that emits events onto the bus' event channel."
  [{:keys [<event]}]
  (fn [[id _ :as event]]
    (log/debug "emitting event:" id)
    (go (a/>! <event event))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord SideEffector [config-opts bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (let [emit-event! (make-emit-event! bus)

          chsk-opts (get-in config-opts [:side-effector :chsk-opts])
          {:keys [ch-recv send-fn]} (sente/make-channel-socket! "/chsk" chsk-opts)]

      (sente/start-chsk-router! ch-recv (make-receive emit-event!))


      (assoc component
             :emit-event! emit-event!
             :send! (make-send! send-fn emit-event!)
             :change-history! (make-change-history! emit-event!))))

  (stop [component] component))
