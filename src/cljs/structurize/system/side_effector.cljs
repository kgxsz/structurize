(ns structurize.system.side-effector
  (:require [bidi.bidi :as b]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [structurize.components.macros :refer [log-info log-debug log-error]]))


;; side-effect handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-side-effect (fn [_ id _] id))


(defmethod process-side-effect :default
  [_ id _]
  (log/warn "failed to process side-effect:" id))


;; exposed functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn side-effect!
  "Dispatches a side-effect into the channel, to be picked
   up by the appropriate listnener and processed."

  ([Φ id] (side-effect! Φ id {}))
  ([{:keys [<side-effects] :as Φ} id props]
   (go (a/>! <side-effects [Φ id props]))))


;; helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn listen-for-side-effects
  [<side-effects]

  (go-loop []
    (let [[{:keys [config-opts !state context] :as Φ} id props] (a/<! <side-effects)
          log? (get-in config-opts [:tooling :log?])
          time-travelling? (l/view-single @!state (in [:tooling :time-travelling?]))
          tooling? (:tooling? context)
          browser? (:browser? context)
          comms? (:comms? context)]

      (cond
        (or browser? comms?) (do
                               (log/debug "side-effect:" id)
                               (process-side-effect Φ id props))

        tooling? (do
                   (when log? (log/debug "side-effect:" id))
                   (process-side-effect Φ id props))

        (not time-travelling?) (do
                                 (log/debug "side-effect:" id)
                                 (process-side-effect Φ id props))

        :else (log/debug "during time travel, ignoring side-effect:" id)))

    (recur)))



;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord SideEffector [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (let [<side-effects (a/chan)]
      (log/info "begin listening for side effects")
      (listen-for-side-effects <side-effects)
      (assoc component :<side-effects <side-effects)))

  (stop [component] component))
