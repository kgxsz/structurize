(ns structurize.system.side-effector
  (:require [com.stuartsierra.component :as component]
            [reagent.ratom :as rr]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method side-effect handling


(defmulti process-side-effect (fn [_ _ _ [id _]] id))


(defmethod process-side-effect :tooling/toggle-tooling-active
  [config-opts state side-effects side-effect]
  "Initialise the state browser by marking the cursored nodes."
  (let [{:keys [emit-mutation!]} side-effects]
    (emit-mutation! [:tooling/toggle-tooling-active {:tooling? true
                                                     :Δ (fn [c] (update-in c [:tooling :tooling-active?] not))}])))


(defmethod process-side-effect :tooling/state-browser-init
  [config-opts state side-effects side-effect]
  "Initialise the state browser by marking the cursored nodes."
  (let [{:keys [!state-browser-props]} state
        {:keys [emit-mutation!]} side-effects
        [id _] side-effect
        cursor-paths (for [c (vals state) :when (instance? rr/RCursor c)] (.-path c))
        add-prop (fn [s prop] (if s (conj s prop) #{prop}))]

    (emit-mutation! [:state-browser/init-cursored {:cursor !state-browser-props
                                                   :tooling? true
                                                   :Δ (fn [state-browser-props]
                                                        (reduce (fn [a v] (update a v add-prop :cursored))
                                                                state-browser-props
                                                                cursor-paths))}])))

(defmethod process-side-effect :default
  [_ _ _ [id _]]
  (log/debug "failed to process side-effect:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; side-effector setup


(defn make-emit-side-effect
  "Returns a function that receives a side-effect and processes it appropriately via multimethods"
  [config-opts state side-effects]
  (fn [[id _ :as side-effect]]
    (log/debug "emitting side-effect:" id)
    (process-side-effect config-opts state side-effects side-effect)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord SideEffector [config-opts browser comms state]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (let [side-effects {:emit-mutation! (get-in state [:mutators :emit-mutation!])
                          :admit-throttled-mutations! (get-in state [:mutators :admit-throttled-mutations!])
                          :send! (:send! comms)
                          :post! (:post! comms)
                          :change-location! (:change-location! browser)}]

      (assoc component
             :emit-side-effect! (make-emit-side-effect config-opts state side-effects)
             :emit-mutation! (get-in state [:mutators :emit-mutation!])
             :admit-throttled-mutations! (get-in state [:mutators :admit-throttled-mutations!])
             :send! (:send! comms)
             :post! (:post! comms)
             :change-location! (:change-location! browser))))

  (stop [component] component))
