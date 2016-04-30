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
    (emit-mutation! [:tooling/toggle-tooling-active {:Δ (fn [db] (update-in db [:tooling :tooling-active?] not))}])))


(defmethod process-side-effect :tooling/cursor-browser-init
  [{:keys [config-opts state comms browser]} id args]

  "Initialise the cursor browser by collecting the global cursors."

  (let [{:keys [emit-mutation!]} state
        cursors (for [[k v] state :when (instance? rr/RCursor v)] [k (.-path v)])]

    (emit-mutation! [:tooling/cursor-browser-init {:Δ (fn [db] (assoc-in db [:tooling :cursors] cursors))}])))


(defmethod process-side-effect :tooling/toggle-node-collapsed
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation!]} state
        {:keys [path]} args]

    (emit-mutation! [:tooling/toggle-node-collapsed {:Δ (fn [db]
                                                          (update-in db [:tooling :state-browser-props :collapsed] #(if (contains? % path)
                                                                                                                      (disj % path)
                                                                                                                      (conj % path))))}])))


(defmethod process-side-effect :tooling/toggle-node-cursored
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation!]} state
        {:keys [path]} args]

    (emit-mutation! [:tooling/toggle-node-cursored {:Δ (fn [db]
                                                         (-> db
                                                             (update-in [:tooling :state-browser-props :cursored :paths] #(if (empty? %) #{path} #{}))
                                                             (update-in [:tooling :state--browser-props :cursored :upstream-paths] #(if (empty? %) (upstream-paths #{path}) #{}))))}])))


(defmethod process-side-effect :tooling/toggle-node-focused
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation!]} state
        {:keys [path]} args]

    (emit-mutation! [:tooling/toggle-node-focused {:Δ (fn [db]
                                                        (-> db
                                                            (update-in [:tooling :state-browser-props :focused :paths] #(if (empty? %) #{path} #{}))
                                                            (update-in [:tooling :state-browser-props :focused :upstream-paths] #(if (empty? %) (upstream-paths #{path}) #{}))))}])))


(defmethod process-side-effect :tooling/back-in-time
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation!]} state]

    (emit-mutation! [:tooling/back-in-time {:Δ (fn [db]
                                                 (let [processed-mutations (get-in db [:tooling :processed-mutations])
                                                       latest-processed-mutation (first processed-mutations)
                                                       second-latest-processed-mutation (second processed-mutations)
                                                       [_ {:keys [diff]}] latest-processed-mutation
                                                       [_ {:keys [mutation-paths upstream-mutation-paths]}] second-latest-processed-mutation]
                                                   (as-> db db
                                                       (update-in db [:tooling :unprocessed-mutations] conj latest-processed-mutation)
                                                       (update-in db [:tooling :processed-mutations] rest)
                                                       (assoc-in db [:tooling :state-browser-props :mutated :paths] mutation-paths)
                                                       (assoc-in db [:tooling :state-browser-props :mutated :upstream-paths] upstream-mutation-paths)
                                                       (reduce (fn [db [path {:keys [before]}]] (assoc-in db path before)) db diff))))}])))


(defmethod process-side-effect :tooling/forward-in-time
  [{:keys [config-opts state comms browser]} id args]
  (let [{:keys [emit-mutation!]} state]

    (emit-mutation! [:tooling/forward-in-time {:Δ (fn [db]
                                                    (let [next-unprocessed-mutation (first (get-in db [:tooling :unprocessed-mutations]))
                                                          [_ {:keys [mutation-paths upstream-mutation-paths diff]}] next-unprocessed-mutation]
                                                      (as-> db db
                                                          (update-in db [:tooling :processed-mutations] conj next-unprocessed-mutation)
                                                          (update-in db [:tooling :unprocessed-mutations] rest)
                                                          (assoc-in db [:tooling :state-browser-props :mutated :paths] mutation-paths)
                                                          (assoc-in db [:tooling :state-browser-props :mutated :upstream-paths] upstream-mutation-paths)
                                                          (reduce (fn [db [path {:keys [after]}]] (assoc-in db path after)) db diff))))}])))


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
        {:keys [path item-name]} args
        mutation-id (keyword  (str "playground/inc-" item-name))]
    (emit-mutation! [mutation-id {:Δ (fn [db] (update-in db path inc))}])))


(defmethod process-side-effect :default
  [_ id _]
  (log/debug "failed to process side-effect:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; side-effector setup


(defn make-emit-side-effect
  "Returns a function that receives a side-effect and processes it appropriately via multimethods"
  [{:keys [config-opts state] :as Φ}]
  (fn [[id args :as side-effect]]
    (let [{:keys [!db]} state
          tooling? (= (namespace id) "tooling")
          real-time? (empty? (get-in @!db [:tooling :unprocessed-mutations]))]

      (if real-time?

        (let [log? (or (not tooling?) (get-in config-opts [:general :tooling :log?]))]
          (when log? (log/debug "emitting side-effect:" id))
          (process-side-effect Φ id args))

        (if tooling?
          (process-side-effect Φ id args)
          (log/debug "while time travelling, ignoring side-effect:" id))))))


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
