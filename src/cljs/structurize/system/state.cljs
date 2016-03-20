(ns structurize.system.state
  (:require [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn make-global-state [{:keys [general]}]
  (r/atom {:message-status {}
           :message-reply {}
           :post-status {}
           :post-response {}
           :location {:path nil
                      :handler :init
                      :query nil}
           :chsk-status :init
           :tooling {:tooling-collapsed? true
                     :node-properties {[:tooling :node-properties] #{:node-collapsed}}}}))


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising state")
    (let [!core (make-global-state config-opts)]
      (assoc component
             :!core !core
             :!handler (r/cursor !core [:location :handler])
             :!query (r/cursor !core [:location :query])
             :!chsk-status (r/cursor !core [:chsk-status])
             :!focused-node (r/cursor !core [:tooling :focused-node]))))

  (stop [component] component))
