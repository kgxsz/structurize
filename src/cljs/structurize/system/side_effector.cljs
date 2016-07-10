(ns structurize.system.side-effector
  (:require [structurize.routes :refer [routes]]
            [structurize.system.system-utils :as u]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method side-effect handling


(defmulti process-side-effect (fn [_ id _] id))


(defmethod process-side-effect :tooling/toggle-tooling-active
  [{:keys [write-tooling!]} id props]
  (write-tooling! [:tooling/toggle-tooling-active
                   (fn [t]
                     (update-in t [:tooling-active?] not))]))


(defmethod process-side-effect :tooling/toggle-node-collapsed
  [{:keys [write-tooling!]} id props]
  (let [{:keys [path]} props]
    (write-tooling! [:tooling/toggle-node-collapsed
                     (fn [t]
                       (update-in t [:app-browser-props :collapsed]
                                  #(if (contains? % path)
                                     (disj % path)
                                     (conj % path))))])))


(defmethod process-side-effect :tooling/toggle-node-focused
  [{:keys [write-tooling!]} id props]
  (let [{:keys [path]} props]
    (write-tooling! [:tooling/toggle-node-focused
                     (fn [t]
                       (-> t
                           (update-in [:app-browser-props :focused :paths]
                                      #(if (empty? %) #{path} #{}))
                           (update-in [:app-browser-props :focused :upstream-paths]
                                      #(if (empty? %) (u/make-upstream-paths #{path}) #{}))))])))


#_(defmethod process-side-effect :tooling/go-back-in-time
    [{:keys [write-app!]} id props]
    (write-app! [:tooling/back-in-time
                     {:Δ (fn [db]
                           (let [processed-mutations (get-in db [:tooling :processed-mutations])
                                 latest-processed-mutation (first processed-mutations)
                               [_ {:keys [pre-Δ-db pre-Δ-mutation-paths pre-Δ-upstream-mutation-paths]}] latest-processed-mutation]
                           (-> pre-Δ-db
                               (assoc :tooling (:tooling db))
                               (update-in [:tooling :unprocessed-mutations] conj latest-processed-mutation)
                               (update-in [:tooling :processed-mutations] rest)
                               (assoc-in [:tooling :app-browser-props :mutated :paths] pre-Δ-mutation-paths)
                               (assoc-in [:tooling :app-browser-props :mutated :upstream-paths]  pre-Δ-upstream-mutation-paths))))}]))


#_(defmethod process-side-effect :tooling/go-forward-in-time
  [{:keys [write-app!]} id props]
  (write-app! [:tooling/forward-in-time
                   {:Δ (fn [db]
                         (let [next-unprocessed-mutation (first (get-in db [:tooling :unprocessed-mutations]))
                               [_ {:keys [post-Δ-db post-Δ-mutation-paths post-Δ-upstream-mutation-paths]}] next-unprocessed-mutation]
                           (-> post-Δ-db
                               (assoc :tooling (:tooling db))
                               (update-in [:tooling :processed-mutations] conj next-unprocessed-mutation)
                               (update-in [:tooling :unprocessed-mutations] rest)
                               (assoc-in [:tooling :app-browser-props :mutated :paths]  post-Δ-mutation-paths)
                               (assoc-in [:tooling :app-browser-props :mutated :upstream-paths] post-Δ-upstream-mutation-paths))))}]))


#_(defmethod process-side-effect :tooling/stop-time-travelling
  [{:keys [write-app!]} id props]
  (write-app! [:tooling/real-time
                   {:Δ (fn [db]
                         (let [latest-unprocessed-mutation (last (get-in db [:tooling :unprocessed-mutations]))
                               unprocessed-mutations (get-in db [:tooling :unprocessed-mutations])
                               [_ {:keys [post-Δ-db post-Δ-mutation-paths post-Δ-upstream-mutation-paths]}] latest-unprocessed-mutation]
                           (-> post-Δ-db
                               (assoc :tooling (:tooling db))
                               (update-in [:tooling :processed-mutations] (partial concat (reverse unprocessed-mutations)))
                               (assoc-in [:tooling :unprocessed-mutations] '())
                               (assoc-in [:tooling :app-browser-props :mutated :paths] post-Δ-mutation-paths)
                               (assoc-in [:tooling :app-browser-props :mutated :upstream-paths] post-Δ-upstream-mutation-paths))))}]))


(defmethod process-side-effect :browser/change-location
  [{:keys [write-app!]} id props]
  (let [{:keys [location]} props]
    (write-app! [:browser/change-location
                 (fn [app]
                   (assoc app :location location))])))


(defmethod process-side-effect :comms/chsk-status-update
  [{:keys [read-app write-app! send!]} id props]
  (let [{chsk-status :status} props
        app-uninitialised? (= :uninitialised
                              (read-app l/view-single
                                        (l/in [:app-status])))]

    (write-app! [:comms/chsk-status-update
                 (fn [app]
                   (assoc-in app [:comms :chsk-status] chsk-status))])

    (when (and (= :open chsk-status) app-uninitialised?)
      (write-app! [:general/app-initialising
                   (fn [app] (assoc app :app-status :initialising))])
      (send! [:general/initialise-app]
             {:on-success (fn [[_ {:keys [me]}]]
                            (write-app! [:general/app-initialised
                                         (fn [app]
                                           (cond-> app
                                             me (assoc-in [:auth :me] me)
                                             true (assoc :app-status :initialised)))]))
              :on-failure (fn [reply]
                            (write-app! [:general/app-initialisation-failed
                                         (fn [app]
                                           (assoc app :app-status :initialisation-failed))]))}))))


(defmethod process-side-effect :comms/message-sent
  [{:keys [write-app!]} id props]
  (let [{:keys [message-id]} props]
    (write-app! [:comms/message-sent
                 (fn [app]
                   (assoc-in app [:comms :message message-id] {:status :sent}))])))


(defmethod process-side-effect :comms/message-reply-received
  [{:keys [write-app!]} id props]
  (let [{:keys [message-id reply on-success]} props]
    (write-app! [:comms/message-reply-received
                 (fn [app]
                   (-> app
                       (assoc-in [:comms :message message-id :status] :reply-received)
                       (assoc-in [:comms :message message-id :reply] (second reply))))])
    (when on-success (on-success reply))))


(defmethod process-side-effect :comms/message-failed
  [{:keys [write-app!]} id props]
  (let [{:keys [message-id reply on-failure]} props]
    (write-app! [:comms/message-failed
                 (fn [app]
                   (-> app
                       (assoc-in [:comms :message message-id :status] :failed)
                       (assoc-in [:comms :message message-id :reply] reply)))])
    (when on-failure (on-failure reply))))


(defmethod process-side-effect :comms/post-sent
  [{:keys [write-app!]} id props]
  (let [{:keys [path]} props]
    (write-app! [:comms/post-sent
                 (fn [app]
                   (assoc-in app [:comms :post path] {:status :sent}))])))


(defmethod process-side-effect :comms/post-response-received
  [{:keys [write-app!]} id props]
  (let [{:keys [path response on-success]} props]
    (write-app! [:comms/post-response-received
                 (fn [app]
                   (-> app
                       (assoc-in [:comms :post path :status] :response-received)
                       (assoc-in [:comms :post path :response] (:?content response))
                       (assoc :app-status :uninitialised)))])
    (when on-success (on-success response))))


(defmethod process-side-effect :comms/post-failed
  [{:keys [write-app!]} id props]
  (let [{:keys [path response on-failure]} props]
    (write-app! [:comms/post-failed
                 (fn [app]
                   (-> app
                       (assoc-in [:comms :post path :status] :failed)
                       (assoc-in [:comms :post path :response] response)))])
    (when on-failure (on-failure response))))


(defmethod process-side-effect :general/change-location
  [{:keys [change-location!]} id props]
  (let [{:keys [path]} props]
    (change-location! {:path path})))


(defmethod process-side-effect :auth/initialise-sign-in-with-github
  [{:keys [send! write-app! change-location!]} id props]
  (send! [:auth/initialise-sign-in-with-github {}]
         {:on-success (fn [[_ {:keys [client-id attempt-id scope redirect-prefix]}]]
                        (let [redirect-uri (str redirect-prefix (b/path-for routes :sign-in-with-github))]
                          (change-location! {:prefix "https://github.com"
                                             :path "/login/oauth/authorize"
                                             :query {:client_id client-id
                                                     :state attempt-id
                                                     :scope scope
                                                     :redirect_uri redirect-uri}})))
          :on-failure (fn [reply]
                        (write-app! [:auth/sign-in-with-github-failed
                                     (fn [app]
                                       (assoc-in app [:auth :sign-in-with-github-failed?] true))]))}))


(defmethod process-side-effect :auth/mount-sign-in-with-github-page
  [{:keys [read-app post! write-app! change-location!]} id props]
  (let [{:keys [code] attempt-id :state} (read-app l/view-single (l/in [:location :query]))]
    (change-location! {:query {} :replace? true})
    (when (and code attempt-id)
      (post! ["/sign-in/github" {:code code :attempt-id attempt-id}]
             {:on-success (fn [response]
                            (change-location! {:path (b/path-for routes :home)}))
              :on-failure (fn [response]
                            (write-app! [:auth/sign-in-with-github-failed
                                         (fn [app]
                                           (assoc-in app [:auth :sign-in-with-github-failed?] true))]))}))))


(defmethod process-side-effect :auth/sign-out
  [{:keys [post! write-app!]} id props]
  (post! ["/sign-out" {}]
         {:on-success (fn [response]
                        (write-app! [:auth/sign-out
                                     (fn [app]
                                       (assoc app :auth {}))]))
          :on-failure (fn [response]
                        (write-app! [:auth/sign-out-failed
                                     (fn [app]
                                       (assoc-in app [:auth :sign-out-status] :failed))]))}))



(defmethod process-side-effect :playground/inc-item
  [{:keys [write-app!]} id props]
  (let [{:keys [path item-name]} props
        mutation-id (keyword (str "playground/inc-" item-name))]
    (write-app! [mutation-id
                 (fn [app]
                   (update-in app path inc))])))


(defmethod process-side-effect :playground/ping
  [{:keys [read-app send! write-app!] :as Φ} id props]
  (let [ping (read-app l/view-single (l/in [:playground :ping]))]

    (write-app! [:playground/ping
                 (fn [app]
                   (update-in app [:playground :ping] inc))])

    (send! [:playground/ping {:ping (inc ping)}]
           {:on-success (fn [[id payload]]
                          (write-app! [:playground/pong
                                       (fn [app]
                                         (assoc-in app [:playground :pong] (:pong payload)))]))
            :on-failure (fn [reply] (write-app! [:playground/ping-failed
                                                (fn [app]
                                                  (assoc-in app [:playground :ping-status] :failed))]))})))


(defmethod process-side-effect :default
  [_ id _]
  (log/warn "failed to process side-effect:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; side-effector setup


(defn listen-for-side-effects

  [{:keys [config-opts !db] :as Φ} <side-effects]

  (let [log? (get-in config-opts [:tooling :log?])]

    (go-loop []
      (let [[id props :as side-effect] (a/<! <side-effects)
            tooling? (= (namespace id) "tooling")]

        (when (or log? (not tooling?))
          (log/debug "side-effect:" id))

        (process-side-effect Φ id props))

      (recur))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord SideEffector [config-opts side-effect-bus browser comms state]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (let [Φ {:config-opts config-opts
             :read-app (:read-app state)
             :read-tooling (:read-tooling state)
             :write-app! (:write-app! state)
             :write-tooling! (:write-tooling! state)
             :send! (:send! comms)
             :post! (:post! comms)
             :change-location! (:change-location! browser)}]

      (log/info "begin listening for side effects")
      (listen-for-side-effects Φ (:<side-effects side-effect-bus))

      component))

  (stop [component] component))
