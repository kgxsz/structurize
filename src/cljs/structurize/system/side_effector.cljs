(ns structurize.system.side-effector
  (:require [structurize.routes :refer [routes]]
            [structurize.system.system-utils :as u]
            [bidi.bidi :as b]
            [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method side-effect handling


(defmulti process-side-effect (fn [_ id _] id))


(defmethod process-side-effect :tooling/toggle-tooling-active
  [{:keys [emit-mutation!]} id props]

  (emit-mutation! [:tooling/toggle-tooling-active
                   {:Δ (fn [db] (update-in db [:tooling :tooling-active?] not))}]))


(defmethod process-side-effect :tooling/toggle-node-collapsed
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [path]} props]
    (emit-mutation! [:tooling/toggle-node-collapsed
                     {:Δ (fn [db]
                           (update-in db [:tooling :state-browser-props :collapsed] #(if (contains? % path)
                                                                                       (disj % path)
                                                                                       (conj % path))))}])))


(defmethod process-side-effect :tooling/toggle-node-focused
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [path]} props]
    (emit-mutation! [:tooling/toggle-node-focused
                     {:Δ (fn [db]
                           (-> db
                               (update-in [:tooling :state-browser-props :focused :paths] #(if (empty? %) #{path} #{}))
                               (update-in [:tooling :state-browser-props :focused :upstream-paths] #(if (empty? %) (u/upstream-paths #{path}) #{}))))}])))


(defmethod process-side-effect :tooling/go-back-in-time
  [{:keys [emit-mutation!]} id props]
  (emit-mutation! [:tooling/back-in-time
                   {:Δ (fn [db]
                         (let [processed-mutations (get-in db [:tooling :processed-mutations])
                               latest-processed-mutation (first processed-mutations)
                               [_ {:keys [pre-Δ-db pre-Δ-mutation-paths pre-Δ-upstream-mutation-paths]}] latest-processed-mutation]
                           (-> db
                             (merge pre-Δ-db)
                             (update-in [:tooling :unprocessed-mutations] conj latest-processed-mutation)
                             (update-in [:tooling :processed-mutations] rest)
                             (assoc-in [:tooling :state-browser-props :mutated :paths] pre-Δ-mutation-paths)
                             (assoc-in [:tooling :state-browser-props :mutated :upstream-paths]  pre-Δ-upstream-mutation-paths))))}]))


(defmethod process-side-effect :tooling/go-forward-in-time
  [{:keys [emit-mutation!]} id props]
  (emit-mutation! [:tooling/forward-in-time
                   {:Δ (fn [db]
                         (let [next-unprocessed-mutation (first (get-in db [:tooling :unprocessed-mutations]))
                               [_ {:keys [post-Δ-db post-Δ-mutation-paths post-Δ-upstream-mutation-paths]}] next-unprocessed-mutation]
                           (-> db
                             (merge post-Δ-db)
                             (update-in [:tooling :processed-mutations] conj next-unprocessed-mutation)
                             (update-in [:tooling :unprocessed-mutations] rest)
                             (assoc-in [:tooling :state-browser-props :mutated :paths]  post-Δ-mutation-paths)
                             (assoc-in [:tooling :state-browser-props :mutated :upstream-paths] post-Δ-upstream-mutation-paths))))}]))


(defmethod process-side-effect :tooling/stop-time-travelling
  [{:keys [emit-mutation!]} id props]
  (emit-mutation! [:tooling/real-time
                   {:Δ (fn [db]
                         (let [latest-unprocessed-mutation (last (get-in db [:tooling :unprocessed-mutations]))
                               unprocessed-mutations (get-in db [:tooling :unprocessed-mutations])
                               [_ {:keys [post-Δ-db post-Δ-mutation-paths post-Δ-upstream-mutation-paths]}] latest-unprocessed-mutation]
                           (-> db
                               (merge post-Δ-db)
                               (update-in [:tooling :processed-mutations] (partial concat (reverse unprocessed-mutations)))
                               (assoc-in [:tooling :unprocessed-mutations] '())
                               (assoc-in [:tooling :state-browser-props :mutated :paths] post-Δ-mutation-paths)
                               (assoc-in [:tooling :state-browser-props :mutated :upstream-paths] post-Δ-upstream-mutation-paths))))}]))


(defmethod process-side-effect :browser/change-location
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [location]} props]
    (emit-mutation! [:browser/change-location
                     {:Δ (fn [db] (assoc db :location location))}])))


(defmethod process-side-effect :comms/chsk-status-update
  [{:keys [emit-mutation!]} id props]=
  (let [{:keys [status]} props]
    (emit-mutation! [:comms/chsk-status-update
                     {:Δ (fn [db] (assoc-in db [:comms :chsk-status] status))}])))


(defmethod process-side-effect :comms/message-sent
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [message-id]} props]
    (emit-mutation! [:comms/message-sent
                     {:Δ (fn [db] (assoc-in db [:comms :message message-id] {:status :sent}))}])))


(defmethod process-side-effect :comms/message-reply-received
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [message-id reply on-success]} props]
    (emit-mutation! [:comms/message-reply-received
                     {:Δ (fn [db] (-> db
                                     (assoc-in [:comms :message message-id :status] :reply-received)
                                     (assoc-in [:comms :message message-id :reply] (second reply))))}])
    (when on-success (on-success reply))))


(defmethod process-side-effect :comms/message-failed
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [message-id reply on-failure]} props]
    (emit-mutation! [:comms/message-failed {:Δ (fn [db] (-> db
                                                           (assoc-in [:comms :message message-id :status] :failed)
                                                           (assoc-in [:comms :message message-id :reply] reply)))}])
    (when on-failure (on-failure reply))))


(defmethod process-side-effect :comms/post-sent
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [path]} props]
    (emit-mutation! [:comms/post-sent {:Δ (fn [db] (assoc-in db [:comms :post path] {:status :sent}))}])))


(defmethod process-side-effect :comms/post-response-received
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [path response on-success]} props]
    (emit-mutation! [:comms/post-response-received {:Δ (fn [db] (-> db
                                                                   (assoc-in [:comms :post path :status] :response-received)
                                                                   (assoc-in [:comms :post path :response] (:?content response))
                                                                   (assoc-in [:comms :chsk-status] :closed)
                                                                   (assoc-in [:comms :message :general/init] nil)))}])
    (when on-success (on-success response))))


(defmethod process-side-effect :comms/post-failed
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [path response on-failure]} props]
    (emit-mutation! [:comms/post-failed {:Δ (fn [db] (-> db
                                                        (assoc-in [:comms :post path :status] :failed)
                                                        (assoc-in [:comms :post path :response] response)))}])
    (when on-failure (on-failure response))))


(defmethod process-side-effect :general/general-init
  [{:keys [send!]} id props]
  (send! [:general/init]))


(defmethod process-side-effect :general/change-location
  [{:keys [change-location!]} id props]
  (let [{:keys [path]} props]
    (change-location! {:path path})))


(defmethod process-side-effect :general/redirect-to-github
  [{:keys [config-opts change-location! !db]} id props]
  (let [host (get-in config-opts [:host])
        {:keys [client-id attempt-id scope redirect-uri]} (get-in @!db [:comms :message :sign-in/init-sign-in-with-github :reply])
        redirect-uri (str redirect-uri (b/path-for routes :sign-in-with-github))]

    (change-location! {:prefix "https://github.com"
                       :path "/login/oauth/authorize"
                       :query {:client_id client-id
                               :state attempt-id
                               :scope scope
                               :redirect_uri redirect-uri}})))


(defmethod process-side-effect :general/init-sign-in-with-github
  [{:keys [send!]} id props]
  (send! [:sign-in/init-sign-in-with-github {}]))


(defmethod process-side-effect :general/sign-in-with-github
  [{:keys [post! change-location!]} id props]
  (change-location! {:query {} :replace? true})
  (post! ["/sign-in/github" props]))


(defmethod process-side-effect :general/sign-out
  [{:keys [post!]} id props]
  (post! ["/sign-out" {}]))


(defmethod process-side-effect :playground/inc-item
  [{:keys [emit-mutation!]} id props]
  (let [{:keys [path item-name]} props
        mutation-id (keyword  (str "playground/inc-" item-name))]
    (emit-mutation! [mutation-id {:Δ (fn [db] (update-in db path inc))}])))


(defmethod process-side-effect :playground/ping
  [{:keys [!db send! emit-mutation!] :as Φ} id props]
  (let [ping (get-in @!db [:playground :ping])]

    (emit-mutation! [:playground/ping {:Δ (fn [db] (update-in db [:playground :ping] inc))}])

    (send! [:playground/ping {:ping (inc ping)}]
           {:on-success (fn [[id payload :as reply]] (emit-mutation! [:playground/pong
                                                                     {:Δ (fn [db] (assoc-in db [:playground :pong] (:pong payload)))}]))
            :on-failure (fn [reply] (emit-mutation! [:playground/ping-failed
                                                    {:Δ (fn [db] (assoc-in db [:playground :ping-status] :failed))}]))})))


(defmethod process-side-effect :default
  [_ id _]
  (log/debug "failed to process side-effect:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; side-effector setup


(defn listen-for-side-effects

  [{:keys [config-opts !db] :as Φ} <side-effects]

  (let [log-tooling? (get-in config-opts [:general :tooling :log?])]

    (go-loop []
      (let [[id props :as side-effect] (a/<! <side-effects)
            tooling? (= (namespace id) "tooling")
            comms? (= (namespace id) "comms")
            browser? (= (namespace id) "browser")
            real-time? (empty? (get-in @!db [:tooling :unprocessed-mutations]))]

        (cond
          tooling? (do
                     (when log-tooling? (log/debug "emitting side-effect:" id))
                     (process-side-effect Φ id props))

          (or comms? browser? real-time?) (do
                                            (log/debug "emitting side-effect:" id)
                                            (process-side-effect Φ id props))

          :else (log/debug "while time travelling, ignoring side-effect:" id)))

      (recur))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord SideEffector [config-opts side-effect-bus browser comms state]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (let [Φ {:config-opts config-opts
             :!db (:!db state)
             :emit-mutation! (:emit-mutation! state)
             :send! (:send! comms)
             :post! (:post! comms)
             :change-location! (:change-location! browser)}]

      (log/info "begin listening for side effects")
      (listen-for-side-effects Φ (:<side-effects side-effect-bus))

      component))

  (stop [component] component))
