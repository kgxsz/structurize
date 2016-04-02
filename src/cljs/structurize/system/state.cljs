(ns structurize.system.state
  (:require [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn make-db [config-opts]
  (r/atom {:playground {:heart 0
                        :star 3}
           :location {:path nil
                      :handler :init
                      :query nil}
           :comms {:chsk-status :init
                   :message {}
                   :post {}}
           :tooling {:tooling-active? true
                     :throttle-events? true
                     :throttled-events '()
                     :processed-events '()
                     :state-browser-props {[:tooling :processed-events] #{:collapsed}
                                           [:tooling :throttled-events] #{:collapsed}
                                           [:tooling :state-browser-props] #{:collapsed}}}}))


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising state")
    (let [!db (make-db config-opts)]
      (assoc component
             :!db !db
             :!handler (r/cursor !db [:location :handler])
             :!query (r/cursor !db [:location :query])
             :!chsk-status (r/cursor !db [:comms :chsk-status])
             :!throttle-events? (r/cursor !db [:tooling :throttle-events?])
             :!throttled-events (r/cursor !db [:tooling :throttled-events])
             :!processed-events (r/cursor !db [:tooling :processed-events])
             :!state-browser-props (r/cursor !db [:tooling :state-browser-props]))))

  (stop [component] component))
