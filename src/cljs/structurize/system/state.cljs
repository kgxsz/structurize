(ns structurize.system.state
  (:require [com.stuartsierra.component :as component]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(defn make-global-state [{:keys [general]}]
  (r/atom {:message-status {}
           :message-reply {}
           :location {:path nil
                      :handler :init
                      :query nil}
           :auth-request-status {}
           :chsk-status :init
           :click-count {:a (:init-click-count-a general)
                         :b 0}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


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
             :!click-count-a (r/cursor !core [:click-count :a])
             :!click-count-b (r/cursor !core [:click-count :b]))))

  (stop [component] component))
