(ns structurize.system.side-effector
  (:require [bidi.bidi :as b]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; side-effect handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-side-effect (fn [_ id _] id))


(defmethod process-side-effect :default
  [_ id _]
  (log/warn "failed to process side-effect:" id))


;; exposed functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn side-effect!

  "Dispatches a side-effect depending on the situation. Always allow tooling, browser and
   comms side effects. Otherwise ignore side effects when time travelling."

  ([Φ id] (side-effect! Φ id {}))
  ([{:keys [config-opts !state context] :as Φ} id props]
   (let [log? (get-in config-opts [:tooling :log?])
         time-travelling? (l/view-single @!state (in [:tooling :time-travelling?]))
         tooling? (:tooling? context)
         browser? (:browser? context)
         comms? (:comms? context)]

     (cond
       (or browser? comms? tooling?) (do
                                       (log-debug Φ "side-effect:" id)
                                       (process-side-effect Φ id props))

       (not time-travelling?) (do
                                (log-debug Φ "side-effect:" id)
                                (process-side-effect Φ id props))

       :else (log-debug Φ "during time travel, ignoring side-effect:" id)))))

;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord SideEffector []
  component/Lifecycle
  (start [component] component)
  (stop [component] component))
