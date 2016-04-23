(ns structurize.system.side-effector
  (:require [bidi.bidi :as b]
            [com.stuartsierra.component :as component]
            [structurize.routes :refer [routes]]
            [reagent.ratom :as rr]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; cheeky helpers


(defn upstream-paths [paths]
  (->> paths
       (map drop-last)
       (remove empty?)
       (map (partial reductions conj []))
       (map rest)
       (apply concat)
       set))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method side-effect handling


(defmulti process-side-effect (fn [_ id _] id))


(defmethod process-side-effect :tooling/toggle-tooling-active
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation!]} state]
    (emit-mutation! [:tooling/toggle-tooling-active {:Δ (fn [c] (update-in c [:tooling :tooling-active?] not))}])))


(defmethod process-side-effect :tooling/cursor-browser-init
  [{:keys [config-opts state comms browser]} id args]

  "Initialise the cursor browser by collecting the global cursors."

  (let [{:keys [emit-mutation!]} state
        cursors (for [[k v] state :when (instance? rr/RCursor v)] [k (.-path v)])]

    (emit-mutation! [:tooling/cursor-browser-init {:Δ (fn [c] (assoc-in c [:tooling :cursors] cursors))}])))


(defmethod process-side-effect :tooling/disable-mutations-throttling
  [{:keys [config-opts state comms browser]} id args]

  "Disables mutation throttling, ensure that
   all outstanding mutations are flushed out."

  (let [{:keys [!throttle-mutations? emit-mutation! admit-throttled-mutations!]} state]

    (admit-throttled-mutations!)
    (emit-mutation! [:tooling/disable-throttle-mutations {:cursor !throttle-mutations?
                                                          :Δ (constantly false)}])))


(defmethod process-side-effect :tooling/enable-mutations-throttling
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [!throttle-mutations? emit-mutation!]} state]
    (emit-mutation! [:tooling/disable-throttle-mutations {:cursor !throttle-mutations?
                                                          :Δ (constantly true)}])))


(defmethod process-side-effect :tooling/admit-next-throttled-mutation
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [admit-throttled-mutations!]} state]
    (admit-throttled-mutations! 1)))


(defmethod process-side-effect :tooling/toggle-node-collapsed
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation! !state-browser-props]} state
        {:keys [path]} args]

    (emit-mutation! [:tooling/toggle-node-collapsed {:cursor !state-browser-props
                                                     :Δ (fn [c]
                                                          (update-in c [:collapsed] #(if (contains? % path)
                                                                                       (disj % path)
                                                                                       (conj % path))))}])))


(defmethod process-side-effect :tooling/toggle-node-cursored
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation! !state-browser-props]} state
        {:keys [path]} args]

    (emit-mutation! [:tooling/toggle-node-cursored {:cursor !state-browser-props
                                                    :Δ (fn [c]
                                                         (-> c
                                                             (update-in [:cursored :paths] #(if (empty? %) #{path} #{}))
                                                             (update-in [:cursored :upstream-paths] #(if (empty? %) (upstream-paths #{path}) #{}))))}])))


(defmethod process-side-effect :tooling/toggle-node-focused
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [!state-browser-props emit-mutation!]} state
        {:keys [path]} args]

    (emit-mutation! [:tooling/toggle-node-focused {:cursor !state-browser-props
                                                   :Δ (fn [c]
                                                        (-> c
                                                            (update-in [:focused :paths] #(if (empty? %) #{path} #{}))
                                                            (update-in [:focused :upstream-paths] #(if (empty? %) (upstream-paths #{path}) #{}))))}])))


(defmethod process-side-effect :general/general-init
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [send!]} comms]
    (send! [:general/init])))


(defmethod process-side-effect :general/change-location
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [change-location!]} browser
        {:keys [path]} args]
    (change-location! {:path path})))


(defmethod process-side-effect :general/redirect-to-github
  [{:keys [config-opts state comms browser]} id args]
  (let [host (get-in config-opts [:host])
        {:keys [change-location!]} browser
        {:keys [client-id attempt-id scope]} args
        redirect-uri (str host (b/path-for routes :sign-in-with-github))]

    (change-location! {:prefix "https://github.com"
                       :path "/login/oauth/authorize"
                       :query {:client_id client-id
                               :state attempt-id
                               :scope scope
                               :redirect_uri redirect-uri}})))


(defmethod process-side-effect :general/init-sign-in-with-github
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [send!]} comms]
    (send! [:sign-in/init-sign-in-with-github {}])))


(defmethod process-side-effect :general/sign-in-with-github
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [post!]} comms
        {:keys [change-location!]} browser]
    (change-location! {:query {} :replace? true})
    (post! ["/sign-in/github" args])))


(defmethod process-side-effect :general/sign-out
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [post!]} comms]
    (post! ["/sign-out" {}])))


(defmethod process-side-effect :playground/inc-item
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation!]} state
        {:keys [cursor item-name]} args
        mutation-id (keyword  (str "playground/inc-" item-name))]
    (emit-mutation! [mutation-id {:cursor cursor :Δ inc}])))


(defmethod process-side-effect :default
  [_ _ _ [id _]]
  (log/debug "failed to process side-effect:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; side-effector setup


(defn make-emit-side-effect
  "Returns a function that receives a side-effect and processes it appropriately via multimethods"
  [Φ]
  (fn [[id args :as side-effect]]
    (let [log? (not= (namespace id) "tooling")]
      (when log? (log/debug "emitting side-effect:" id))
      (process-side-effect Φ id args))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord SideEffector [config-opts browser comms state]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (let [Φ {:config-opts config-opts
             :state state
             :comms comms
             :browser browser}]
      (assoc component
             :emit-side-effect! (make-emit-side-effect Φ))))

  (stop [component] component))
