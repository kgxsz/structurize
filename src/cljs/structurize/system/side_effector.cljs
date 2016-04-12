(ns structurize.system.side-effector
  (:require [bidi.bidi :as b]
            [com.stuartsierra.component :as component]
            [structurize.routes :refer [routes]]
            [reagent.ratom :as rr]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; cheeky helpers


(defn add-prop [s prop]
  (if s
    (conj s prop)
    #{prop}))


(defn remove-prop [s prop]
  (if s
    (disj s prop)
    #{}))


(defn toggle-prop [s prop]
  (if (contains? s prop)
    (remove-prop s prop)
    (add-prop s prop)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method side-effect handling


(defmulti process-side-effect (fn [_ _ _ [id _]] id))


(defmethod process-side-effect :tooling/toggle-tooling-active
  [config-opts state side-effects side-effect]

  "Collapse"

  (let [{:keys [emit-mutation!]} side-effects]
    (emit-mutation! [:tooling/toggle-tooling-active {:tooling? true
                                                     :Δ (fn [c] (update-in c [:tooling :tooling-active?] not))}])))


(defmethod process-side-effect :tooling/state-browser-init
  [config-opts state side-effects side-effect]

  "Initialise the state browser by marking the cursored nodes."

  (let [{:keys [!state-browser-props]} state
        {:keys [emit-mutation!]} side-effects
        cursor-paths (for [c (vals state) :when (instance? rr/RCursor c)] (.-path c))]

    (emit-mutation! [:tooling/state-browser-init {:cursor !state-browser-props
                                                  :tooling? true
                                                  :Δ (fn [state-browser-props]
                                                       (reduce (fn [a v] (update a v add-prop :cursored))
                                                               state-browser-props
                                                               cursor-paths))}])))


(defmethod process-side-effect :tooling/disable-mutations-throttling
  [config-opts state side-effects side-effect]

  "Disables mutation throttling, ensure that
   all outstanding mutations are flushed out."

  (let [{:keys [!throttle-mutations?]} state
        {:keys [emit-mutation! admit-throttled-mutations!]} side-effects]

    (admit-throttled-mutations!)
    (emit-mutation! [:tooling/disable-throttle-mutations {:cursor !throttle-mutations?
                                                          :tooling? true
                                                          :Δ (constantly false)}])))


(defmethod process-side-effect :tooling/enable-mutations-throttling
  [config-opts state side-effects side-effect]
  (let [{:keys [!throttle-mutations?]} state
        {:keys [emit-mutation!]} side-effects]
    (emit-mutation! [:tooling/disable-throttle-mutations {:cursor !throttle-mutations?
                                                          :tooling? true
                                                          :Δ (constantly true)}])))


(defmethod process-side-effect :tooling/admit-next-throttled-mutation
  [config-opts state side-effects side-effect]
  (let [{:keys [admit-throttled-mutations!]} side-effects]
    (admit-throttled-mutations! 1)))


(defmethod process-side-effect :tooling/toggle-node-collapsed
  [config-opts state side-effects side-effect]
  (let [{:keys [emit-mutation!]} side-effects
        [_ {:keys [!node-props]}] side-effect]

    (emit-mutation! [:tooling/toggle-node-collapsed {:cursor !node-props
                                                     :tooling? true
                                                     :Δ (fn [c] (toggle-prop c :collapsed))}])))


(defmethod process-side-effect :tooling/toggle-node-focused
  [config-opts state side-effects side-effect]
  (let [{:keys [!state-browser-props]} state
        {:keys [emit-mutation!]} side-effects
        [_ {:keys [path]}] side-effect]

    (emit-mutation! [:state-browser/toggle-node-focused {:cursor !state-browser-props
                                                         :tooling? true
                                                         :Δ (fn [c]
                                                              (as-> c c
                                                                (update c path toggle-prop :focused)
                                                                (reduce (fn [a v] (update a v toggle-prop :upstream-focused))
                                                                        c
                                                                        (-> (reductions conj [] path) rest drop-last))))}])))


(defmethod process-side-effect :general/general-init
  [config-opts state side-effects side-effect]
  (let [{:keys [send!]} side-effects]
    (send! [:general/init])))


(defmethod process-side-effect :general/change-location
  [config-opts state side-effects side-effect]
  (let [{:keys [change-location!]} side-effects
        [_ {:keys [path]}] side-effect]
    (change-location! {:path path})))


(defmethod process-side-effect :general/redirect-to-github
  [config-opts state side-effects side-effect]
  (let [host (get-in config-opts [:host])
        {:keys [change-location!]} side-effects
        [_ {:keys [client-id attempt-id scope]}] side-effect
        redirect-uri (str host (b/path-for routes :sign-in-with-github))]

    (change-location! {:prefix "https://github.com"
                       :path "/login/oauth/authorize"
                       :query {:client_id client-id
                               :state attempt-id
                               :scope scope
                               :redirect_uri redirect-uri}})))


(defmethod process-side-effect :general/init-sign-in-with-github
  [config-opts state side-effects side-effect]
  (let [{:keys [send!]} side-effects]
    (send! [:sign-in/init-sign-in-with-github {}])))


(defmethod process-side-effect :general/sign-in-with-github
  [config-opts state side-effects side-effect]
  (let [{:keys [change-location! post!]} side-effects
        [_ params] side-effect]
    (change-location! {:query {} :replace? true})
    (post! ["/sign-in/github" params])))


(defmethod process-side-effect :general/sign-out
  [config-opts state side-effects side-effect]
  (let [{:keys [post!]} side-effects]
    (post! ["/sign-out" {}])))


(defmethod process-side-effect :playground/inc-item
  [config-opts state side-effects side-effect]
  (let [{:keys [emit-mutation!]} side-effects
        [_ {:keys [cursor item-name]}] side-effect
        mutation-id (keyword  (str ":playground/inc-" item-name))]

    (emit-mutation! [mutation-id {:cursor cursor :Δ inc}])))


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
