(ns structurize.system.side-effector
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method side-effect handling


(defmulti process-side-effect (fn [_ _ side-effect] (first side-effect)))


(defmethod process-side-effect :tooling/toggle-tooling-active
  [config-opts {:keys [emit-mutation!] :as side-effectors} [id params]]
  (emit-mutation! [:tooling/toggle-tooling-active {:tooling? true
                                                   :Δ (fn [c] (update-in c [:tooling :tooling-active?] not))}]))


(defmethod process-side-effect :default
  [_ _ [id _]]
  (log/debug "failed to process side-effect:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; side-effector setup


(defn make-emit-side-effect
  "Returns a function that receives a side-effect and processes it appropriately via multimethods"
  [config-opts side-effectors]
  (fn [[id _ :as side-effect]]
    (log/debug "emitting side-effect:" id)
    (process-side-effect config-opts side-effectors side-effect)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord SideEffector [config-opts browser comms state]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (let [side-effectors {:emit-mutation! (get-in state [:mutators :emit-mutation!])
                          :admit-throttled-mutations! (get-in state [:mutators :admit-throttled-mutations!])
                          :send! (:send! comms)
                          :post! (:post! comms)
                          :change-location! (:change-location! browser)}]

      (assoc component
             :emit-side-effect! (make-emit-side-effect config-opts side-effectors)
             :emit-mutation! (get-in state [:mutators :emit-mutation!])
             :admit-throttled-mutations! (get-in state [:mutators :admit-throttled-mutations!])
             :send! (:send! comms)
             :post! (:post! comms)
             :change-location! (:change-location! browser))))

  (stop [component] component))
