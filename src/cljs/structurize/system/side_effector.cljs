(ns structurize.system.side-effector
  (:require [structurize.system.system-utils :as u]
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


(defmethod process-side-effect :tooling/go-back-in-time
  [{:keys [read-tooling write-tooling!]} id props]
  (let [track-index (max 0 (dec (read-tooling l/view-single (l/in [:track-index]))))
        {:keys [paths upstream-paths]} (read-tooling l/view-single (l/in [:writes track-index]))]
    (write-tooling! [:tooling/go-back-in-time
                     (fn [t]
                       (-> t
                           (assoc :track-index track-index)
                           (assoc-in [:app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})})))])))


(defmethod process-side-effect :tooling/go-forward-in-time
  [{:keys [read-tooling write-tooling!]} id props]
  (let [read-write-index (read-tooling l/view-single (l/in [:read-write-index]))
        track-index (min read-write-index (inc (read-tooling l/view-single (l/in [:track-index]))))
        {:keys [paths upstream-paths]} (read-tooling l/view-single (l/in [:writes track-index]))]
    (write-tooling! [:tooling/go-forward-in-time
                     (fn [t]
                       (-> t
                           (assoc :track-index track-index)
                           (assoc-in [:app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})})))])))


(defmethod process-side-effect :tooling/stop-time-travelling
  [{:keys [read-tooling write-tooling!]} id props]
  (let [read-write-index (read-tooling l/view-single (l/in [:read-write-index]))
        {:keys [paths upstream-paths]} (read-tooling l/view-single (l/in [:writes read-write-index]))]
    (write-tooling! [:tooling/stop-time-travellin
                     (fn [t]
                       (-> t
                           (assoc :track-index read-write-index)
                           (assoc-in [:app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})})))])))


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
  [{:keys [config-opts send! write-app! change-location!]} id props]
  (send! [:auth/initialise-sign-in-with-github {}]
         {:on-success (fn [[_ {:keys [client-id attempt-id scope redirect-prefix]}]]
                        (let [redirect-uri (str redirect-prefix (b/path-for (:routes config-opts) :sign-in-with-github))]
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
  [{:keys [config-opts read-app post! write-app! change-location!]} id props]
  (let [{:keys [code] attempt-id :state} (read-app l/view-single (l/in [:location :query]))]
    (change-location! {:query {} :replace? true})
    (when (and code attempt-id)
      (post! ["/sign-in/github" {:code code :attempt-id attempt-id}]
             {:on-success (fn [response]
                            (change-location! {:path (b/path-for (:routes config-opts) :home)}))
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
  [{:keys [config-opts read-tooling] :as Φ} <side-effects]

  (let [log? (get-in config-opts [:tooling :log?])]

    (go-loop []
      (let [[id props :as side-effect] (a/<! <side-effects)
            real-time? (apply = (read-tooling l/view (l/+> (l/in [:read-write-index]) (l/in [:track-index]))))
            tooling? (= (namespace id) "tooling")
            browser? (= (namespace id) "browser")
            comms? (= (namespace id) "comms")]

        (cond
          (or browser? comms?) (do
                                 (log/debug "side-effect:" id)
                                 (process-side-effect Φ id props))

          tooling? (do
                     (when log? (log/debug "side-effect:" id))
                     (process-side-effect Φ id props))

          real-time? (do
                       (log/debug "side-effect:" id)
                       (process-side-effect Φ id props))

         :else (log/debug "during time travel, ignoring side-effect:" id)))

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
