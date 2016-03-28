(ns structurize.system.state
  (:require [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn make-global-state [{:keys [general]}]
  (r/atom {:playground {:heart 0
                        :star 3}
           :location {:path nil
                      :handler :init
                      :query nil}
           :comms {:chsk-status :init
                   :message {}
                   :post {}}

           :tooling {:tooling-active? true
                     :throttle-events? false
                     :throttled-events '()
                     :processed-events '()
                     :state-browser-props {[:tooling :processed-events] #{:collapsed}
                                           [:tooling :throttled-events] #{:collapsed}
                                           [:tooling :state-browser-props] #{:collapsed}}}}))


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising state")
    (let [!core (make-global-state config-opts)]
      (assoc component
             :!core !core
             :!handler (r/cursor !core [:location :handler])
             :!query (r/cursor !core [:location :query])
             :!chsk-status (r/cursor !core [:comms :chsk-status])
             :!throttle-events? (r/cursor !core [:tooling :throttle-events?])
             :!throttled-events (r/cursor !core [:tooling :throttled-events])
             :!processed-events (r/cursor !core [:tooling :processed-events])
             :!state-browser-props (r/cursor !core [:tooling :state-browser-props]))))

  (stop [component] component))
