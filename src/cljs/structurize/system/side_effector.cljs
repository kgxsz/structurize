(ns structurize.system.side-effector
  (:require [structurize.routes :refer [routes]]
            [bidi.bidi :as b]
            [cemerick.url :refer [map->query query->map]]
            [cljs.core.async :as a]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [goog.events :as events]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log]
            [medley.core :as m])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.core :refer [defroute]])
  (:import [goog.history Html5History EventType]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; history setup


(defn make-navigation-handler
  "Returns a function that handles browser navigation
   events and emits the location-change event, which will
   cause an update to the location information in the state."
  [history emit-event!]

  (fn [g-event]
    (let [token (.getToken history)
          [path query] (str/split token "?")
          location (merge {:path path
                           :query (->> query query->map (m/map-keys keyword))}
                          (b/match-route routes path))]
      (log/debug "received navigation from browser:" token)
      (when-not (.-isNavigation g-event) (js/window.scrollTo 0 0))
      (emit-event! [:location-change {:Δ (fn [core] (assoc core :location location))}]))))


(defn make-transformer
  "Custom transformer required to manage query parameters."
  []
  (let [transformer (Html5History.TokenTransformer.)]
    (set! transformer.retrieveToken
          (fn [path-prefix location]
            (str (.-pathname location) (.-search location))))
    (set! transformer.createUrl
          (fn [token path-prefix location]
            (str path-prefix token)))
    transformer))


(defn make-history []
  (doto (Html5History. js/window (make-transformer))
    (.setPathPrefix "")
    (.setUseFragment false)))


(defn listen-for-navigation [history handler]
  (doto history
    (goog.events/listen EventType.NAVIGATE #(handler %))
    (.setEnabled true)))


(defn make-change-history!
  "Returns a function that takes a map of options and updates the
   browser's navigation accordingly. The browser will fire a navigation
   event if the history changes, which will be dealt with by a listener."
  [history]
  (fn [{:keys [prefix path query replace?]}]
    (let [query-string (when-not (str/blank? (map->query query)) (str "?" (map->query query)))
          current-path (-> (.getToken history) (str/split "?") first)
          token (str (or path current-path) query-string)]
      (log/debug "dispatching navigation to browser:" (str prefix token))
      (cond
        prefix (set! js/window.location (str prefix token))
        replace? (.replaceToken history token)
        :else (.setToken history token)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; comms setup


(defn make-receive
  "Returns a function that receives a message and dispatches it appropriately."
  [emit-event!]

  (fn [{:keys [event id ?data send-fn]}]
    (log/info "received message from server:" id)
    (cond
      (= id :chsk/state) (do (log/info "chsk status change")
                             (emit-event! [:chsk-status-update {:Δ (fn [core] (assoc core :chsk-status (if (:open? ?data) :open :closed)))}]))
      (= id :chsk/handshake) (log/info "chsk handshake"))))


(defn make-send!
  "Returns a function that takes a message to send, an event is emitted
   when the message is dispatched, and another when the message reply is received."
  [send-fn emit-event!]

  (fn [[id _ :as message] {:keys [timeout]}]
    (log/debug "dispatching message to server:" id)
    (emit-event! [:message-sent {:Δ (fn [core] (assoc-in core [:message-status id] :sent))}])

    (send-fn
      message
      (or timeout 10000)
      (fn [reply]
        (if (sente/cb-success? reply)
          (let [[id ?payload] reply]
            (log/debug "received a reply message from server:" id)
            (emit-event! [:message-reply-received {:Δ (fn [core] (-> core
                                                                    (assoc-in [:message-status id] :reply-received)
                                                                    (assoc-in [:message-reply id] ?payload)))}]))
          (do
            (log/warn "message failed with:" reply)
            (emit-event! [:message-failed {:Δ (fn [core] (-> core
                                                            (assoc-in [:message-status id] :failed)
                                                            (assoc-in [:message-reply id] reply)))}])))))))


(defn make-post!
  "Returns a function that makes an ajax post to the server. An event is emitted
   when the request is made, and another when the response is received."
  [chsk chsk-state emit-event!]

  (fn [[path params] {:keys [timeout]}]
    (log/debug "dispatching post to server:" path)
    (emit-event! [:post-sent {:Δ (fn [core] (assoc-in core [:post-status path] :sent))}])

    (sente/ajax-lite
     path
     {:method :post
      :timeout-ms (or timeout 10000)
      :params (merge params (select-keys @chsk-state [:csrf-token]))}
     (fn [response]
       (if (:success? response)
         (do
           (log/debug "received a post response from server:" path)
           (emit-event! [:post-response-received {:Δ (fn [core] (-> core
                                                                   (assoc-in [:post-status path] :response-received)
                                                                   (assoc-in [:post-response path] (:?content response))))}])
           ;; we reconnect the websocket connection here to pick up any changes
           ;; in the session that may have come about with the post request
           (sente/chsk-reconnect! chsk))
         (do
           (log/warn "post failed with:" response)
           (emit-event! [:post-failed {:Δ (fn [core] (-> core
                                                        (assoc-in [:post-status path] :failed)
                                                        (assoc-in [:post-response path] response)))}])))))))


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
          history (make-history)
          {:keys [chsk ch-recv send-fn] chsk-state :state} (sente/make-channel-socket! "/chsk" (get-in config-opts [:side-effector :chsk-opts]))]

      (log/info "begin listening for messages from server")
      (sente/start-chsk-router! ch-recv (make-receive emit-event!))

      (log/info "begin listening for navigation from the browser")
      (listen-for-navigation history (make-navigation-handler history emit-event!))

      (assoc component
             ;; this guy can still give  these side effecting fucntions in a nice bundle, but they
             ;; need to come from things like
             :emit-event! emit-event!
             :reconnect! #(sente/chsk-reconnect! chsk)
             :send! (make-send! send-fn emit-event!)
             :post! (make-post! chsk chsk-state emit-event!)
             :change-history! (make-change-history! history))))

  (stop [component] component))
