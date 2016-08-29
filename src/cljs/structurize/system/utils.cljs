(ns structurize.system.utils
  (:require [clojure.data :as data]
            [cljs-time.core :as t]
            [reagent.core :as r]
            [traversy.lens :as l]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [cljs.core.async :as a]
            [cemerick.url :refer [map->query query->map]]
            [taoensso.sente :as sente])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;; general utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-upstream-paths
  "This function takes paths and returns a set of all sub-paths within them."
  [paths]

  (->> paths
       (map drop-last)
       (remove empty?)
       (map (partial reductions conj []))
       (map rest)
       (apply concat)
       set))


(defn make-all-paths
  "This function takes a map and returns a list of all paths in the map.
   For example {:a 1 :b {:c 2 :d 3}} would give ((:a) (:b :c) (:b :d))."
  [m]

  (if (or (not (map? m)) (empty? m))
    '(())
    (for [[k v] m
          subkey (make-all-paths v)]
      (cons k subkey))))


(defn make-paths
  "This function finds the path to every changed node between the two maps."
  [post pre]

  (let [[added removed _] (data/diff post pre)
        removed-paths (if removed (make-all-paths removed) [])
        added-paths (if added (make-all-paths added) [])]
    (into #{} (concat removed-paths added-paths))))


;; state related functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn track
  "Derefs a reagent track into the app at current track index.
   If in a tooling context, the track will act from the root of state.

   Params:
   v - traversy view or view-single
   +lens - The lens into the app at current track-index (or state root if in a tooling context)
   f - the function whose output change will determine whether the track is triggered"

  ([Φ v +lens] (track Φ v +lens identity))
  ([{:keys [!state context] :as Φ} v +lens f]
   (if (:tooling? context)
     @(r/track #(f (v @!state +lens)))
     @(r/track #(let [index (get-in @!state [:tooling :track-index])]
                 (f (v @!state (l/*> (l/in [:app-history index]) +lens))))))))


(defn read
  "Reads into the app at current read-write index.
   If in a tooling context, the read will act from the root of state.

   Params:
   v - traversy view or view-single
   +lens - The lens into the app at current track-index (or state root if in a tooling context)"
  [{:keys [!state context] :as Φ} v +lens]

  (if (:tooling? context)
    (v @!state +lens)
    (let [index (get-in @!state [:tooling :read-write-index])]
      (v @!state (l/*> (l/in [:app-history index]) +lens)))))


(defn write!
  "Writes into the app at current read-write index.
   If in a tooling context, the write will act from the root of state.

   Params:
   id - the write id, used in tooling
   f - the mutating function"
  [{:keys [config-opts !state context] :as Φ} id f]

  (if (:tooling? context)
    (let [log? (get-in config-opts [:tooling :log?])]
      (when log? (log/debug "write:" id))
      (swap! !state f))
    (let [state @!state
          index (get-in state [:tooling :read-write-index])
          time-travelling? (get-in state [:tooling :time-travelling?])
          pre-app (get-in state [:app-history index])
          post-app (f pre-app)
          paths (make-paths post-app pre-app)
          upstream-paths (make-upstream-paths paths)]

      (log/debug "write:" id)

      (swap! !state #(cond-> %
                       true (update-in [:tooling :read-write-index] inc)
                       (not time-travelling?) (update-in [:tooling :track-index] inc)
                       true (assoc-in [:tooling :writes (inc index)] {:id id
                                                                      :n (inc index)
                                                                      :paths paths
                                                                      :upstream-paths upstream-paths
                                                                      :t (t/now)})
                       (not time-travelling?) (assoc-in [:tooling :app-browser-props :written] {:paths paths
                                                                                                :upstream-paths upstream-paths})
                       true (assoc-in [:app-history (inc index)] post-app))))))


;; side-effector related functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn side-effect!
  "Dispatches a side-effect into the channel, to be picked
   up by the appropriate listnener and processed."

  ([Φ id] (side-effect! Φ id {}))
  ([{:keys [<side-effects] :as Φ} id props]
   (go (a/>! <side-effects [Φ id props]))))


;; browser related functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn change-location!
  "Updates the browser's location accordingly. The browser will fire a navigation
   event if the location changes, which will be dealt with by a listener.

   Params:
   prefix - the part before the path, set it if you want to navigate to a different site
   path - the path you wish to navigate to
   query - map of query params
   replace? - ensures that the browser replaces the current location in history"
  [{:keys [history] :as Φ} {:keys [prefix path query replace?]}]

  (let [query-string (when-not (str/blank? (map->query query)) (str "?" (map->query query)))
        current-path (-> (.getToken history) (str/split "?") first)
        token (str (or path current-path) query-string)]
    (log/debug "dispatching change of location to browser:" (str prefix token))
    (cond
      prefix (set! js/window.location (str prefix token))
      replace? (.replaceToken history token)
      :else (.setToken history token))))


;; comms related functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

