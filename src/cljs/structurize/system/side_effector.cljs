(ns structurize.system.side-effector
  (:require [structurize.system.system-utils :as u]
            [structurize.system.state :refer [read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [post! send!]]
            [structurize.components.general :as g]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; multi-method side-effect handling


(defmulti process-side-effect (fn [_ id _] id))


(defmethod process-side-effect :tooling/toggle-tooling-slide-over
  [Φ id {:keys [+slide-over] :as props}]
  (write! Φ :tooling/toggle-tooling-slide-over
         (fn [x]
           (g/toggle-slide-over! x +slide-over))))


(defmethod process-side-effect :tooling/toggle-node-collapsed
  [Φ id {:keys [path] :as props}]
  (write! Φ :tooling/toggle-node-collapsed
          (fn [x]
            (update-in x [:tooling :app-browser-props :collapsed]
                       #(if (contains? % path)
                          (disj % path)
                          (conj % path))))))


(defmethod process-side-effect :tooling/toggle-node-focused
  [Φ id {:keys [path] :as props}]
  (write! Φ :tooling/toggle-node-focused
         (fn [x]
           (-> x
               (update-in [:tooling :app-browser-props :focused :paths]
                          #(if (empty? %) #{path} #{}))
               (update-in [:tooling :app-browser-props :focused :upstream-paths]
                          #(if (empty? %) (u/make-upstream-paths #{path}) #{}))))))


(defmethod process-side-effect :tooling/go-back-in-time
  [Φ id props]
  (let [track-index (max 0 (dec (read Φ l/view-single
                                      (l/in [:tooling :track-index]))))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (l/in [:tooling :writes track-index]))]
    (write! Φ :tooling/go-back-in-time
            (fn [x]
              (-> x
                  (assoc-in [:tooling :track-index] track-index)
                  (assoc-in [:tooling :app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})}))))))


(defmethod process-side-effect :tooling/go-forward-in-time
  [Φ id props]
  (let [read-write-index (read Φ l/view-single
                               (l/in [:tooling :read-write-index]))
        track-index (min read-write-index (inc (read Φ l/view-single
                                                     (l/in [:tooling :track-index]))))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (l/in [:tooling :writes track-index]))]
    (write! Φ :tooling/go-forward-in-time
            (fn [x]
              (-> x
                  (assoc-in [:tooling :track-index] track-index)
                  (assoc-in [:tooling :app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})}))))))


(defmethod process-side-effect :tooling/stop-time-travelling
  [Φ id props]
  (let [read-write-index (read Φ l/view-single
                               (l/in [:tooling :read-write-index]))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (l/in [:tooling :writes read-write-index]))]
    (write! Φ :tooling/stop-time-travellin
            (fn [x]
              (-> x
                  (assoc-in [:tooling :track-index] read-write-index)
                  (assoc-in [:tooling :app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})}))))))


(defmethod process-side-effect :browser/change-location
  [Φ id {:keys [location] :as props}]
  (write! Φ :browser/change-location
          (fn [x]
            (assoc x :location location))))


(defmethod process-side-effect :comms/chsk-status-update
  [Φ id {chsk-status :status :as props}]
  (let [app-uninitialised? (= :uninitialised
                              (read Φ l/view-single
                                    (l/in [:app-status])))]

    (write! Φ :comms/chsk-status-update
            (fn [x]
              (assoc-in x [:comms :chsk-status] chsk-status)))

    (when (and (= :open chsk-status) app-uninitialised?)
      (write! Φ :general/app-initialising
              (fn [x] (assoc x :app-status :initialising)))
      (send! Φ :general/initialise-app
             {}
             {:on-success (fn [[_ {:keys [me]}]]
                            (write! Φ :general/app-initialised
                                    (fn [x]
                                      (cond-> x
                                        me (assoc-in [:auth :me] me)
                                        true (assoc :app-status :initialised)))))
              :on-failure (fn [reply]
                            (write! Φ :general/app-initialisation-failed
                                    (fn [x]
                                      (assoc x :app-status :initialisation-failed))))}))))


(defmethod process-side-effect :comms/message-sent
  [Φ id {:keys [message-id] :as props}]
  (write! Φ :comms/message-sent
          (fn [x]
            (assoc-in x [:comms :message message-id] {:status :sent}))))


(defmethod process-side-effect :comms/message-reply-received
  [Φ id {:keys [message-id reply on-success] :as props}]
  (write! Φ :comms/message-reply-received
          (fn [x]
            (-> x
                (assoc-in [:comms :message message-id :status] :reply-received)
                (assoc-in [:comms :message message-id :reply] (second reply)))))
  (when on-success (on-success reply)))


(defmethod process-side-effect :comms/message-failed
  [Φ id {:keys [message-id reply on-failure] :as props}]
  (write! Φ :comms/message-failed
          (fn [x]
            (-> x
                (assoc-in [:comms :message message-id :status] :failed)
                (assoc-in [:comms :message message-id :reply] reply))))
  (when on-failure (on-failure reply)))


(defmethod process-side-effect :comms/post-sent
  [Φ id {:keys [path] :as props}]
  (write! Φ :comms/post-sent
          (fn [x]
            (assoc-in x [:comms :post path] {:status :sent}))))



(defmethod process-side-effect :comms/post-response-received
  [Φ id {:keys [path response on-success] :as props}]
  (write! Φ :comms/post-response-received
          (fn [x]
            (-> x
                (assoc-in [:comms :post path :status] :response-received)
                (assoc-in [:comms :post path :response] (:?content response))
                (assoc :app-status :uninitialised))))
  (when on-success (on-success response)))


(defmethod process-side-effect :comms/post-failed
  [Φ id {:keys [path response on-failure] :as props}]
  (write! Φ :comms/post-failed
          (fn [x]
            (-> x
                (assoc-in [:comms :post path :status] :failed)
                (assoc-in [:comms :post path :response] response))))
  (when on-failure (on-failure response)))


(defmethod process-side-effect :general/change-location
  [Φ id {:keys [path] :as props}]
  (change-location! Φ {:path path}))


(defmethod process-side-effect :auth/initialise-sign-in-with-github
  [{:keys [config-opts] :as Φ} id props]
  (send! Φ :auth/initialise-sign-in-with-github
         {}
         {:on-success (fn [[_ {:keys [client-id attempt-id scope redirect-prefix]}]]
                        (let [redirect-uri (str redirect-prefix (b/path-for (:routes config-opts) :sign-in-with-github))]
                          (change-location! Φ {:prefix "https://github.com"
                                               :path "/login/oauth/authorize"
                                               :query {:client_id client-id
                                                       :state attempt-id
                                                       :scope scope
                                                       :redirect_uri redirect-uri}})))
          :on-failure (fn [reply]
                        (write! Φ :auth/sign-in-with-github-failed
                                (fn [x]
                                  (assoc-in x [:auth :sign-in-with-github-failed?] true))))}))


(defmethod process-side-effect :auth/mount-sign-in-with-github-page
  [{:keys [config-opts] :as Φ} id props]
  (let [{:keys [code] attempt-id :state} (read Φ l/view-single
                                               (l/in [:location :query]))]
    (change-location! Φ {:query {} :replace? true})
    (when (and code attempt-id)
      (post! Φ "/sign-in/github"
             {:code code :attempt-id attempt-id}
             {:on-success (fn [response]
                            (change-location! Φ {:path (b/path-for (:routes config-opts) :home)}))
              :on-failure (fn [response]
                            (write! Φ :auth/sign-in-with-github-failed
                                    (fn [x]
                                      (assoc-in x [:auth :sign-in-with-github-failed?] true))))}))))


(defmethod process-side-effect :auth/sign-out
  [Φ id props]
  (post! Φ "/sign-out"
         {}
         {:on-success (fn [response]
                        (write! Φ :auth/sign-out
                                (fn [x]
                                  (assoc x :auth {}))))
          :on-failure (fn [response]
                        (write! Φ :auth/sign-out-failed
                                (fn [x]
                                  (assoc-in x [:auth :sign-out-status] :failed))))}))


(defmethod process-side-effect :playground/inc-item
  [Φ id {:keys [path item-name] :as props}]
  (let [mutation-id (keyword (str "playground/inc-" item-name))]
    (write! Φ mutation-id
            (fn [x]
              (update-in x path inc)))))


(defmethod process-side-effect :playground/ping
  [Φ id props]
  (let [ping (read Φ l/view-single
                   (l/in [:playground :ping]))]

    (write! Φ :playground/ping
            (fn [x]
              (update-in x [:playground :ping] inc)))

    (send! Φ :playground/ping
           {:ping (inc ping)}
           {:on-success (fn [[id payload]]
                          (write! Φ :playground/pong
                                  (fn [x]
                                    (assoc-in x [:playground :pong] (:pong payload)))))
            :on-failure (fn [reply] (write! Φ :playground/ping-failed
                                           (fn [x]
                                             (assoc-in x [:playground :ping-status] :failed))))})))


(defmethod process-side-effect :default
  [_ id _]
  (log/warn "failed to process side-effect:" id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; side-effector setup


(defn listen-for-side-effects
  [<side-effects]

  (go-loop []
    (let [[{:keys [config-opts !state context] :as Φ} id props] (a/<! <side-effects)
          log? (get-in config-opts [:tooling :log?])
          real-time? (apply = (l/view @!state (l/+> (l/in [:tooling :read-write-index])
                                                    (l/in [:tooling :track-index]))))
          browser? (= (namespace id) "browser")
          comms? (= (namespace id) "comms")]

      (cond
        (or browser? comms?) (do
                               (log/debug "side-effect:" id)
                               (process-side-effect Φ id props))

        (:tooling? context) (do
                              (when log? (log/debug "side-effect:" id))
                              (process-side-effect Φ id props))

        real-time? (do
                     (log/debug "side-effect:" id)
                     (process-side-effect Φ id props))

        :else (log/debug "during time travel, ignoring side-effect:" id)))

    (recur)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord SideEffector [config-opts side-effect-bus]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effector")
    (log/info "begin listening for side effects")
    (listen-for-side-effects (:<side-effects side-effect-bus))
    component)

  (stop [component] component))
