(ns structurize.system.state
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defn make-global-state [{:keys [general]}]
  (r/atom {:message-status {}
           :message-reply {}
           :click-count {:a (:init-click-count-a general)
                         :b 0}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Initialising state")
    (let [!core (make-global-state config-opts)]
      (assoc component
             :!core !core
             :!click-count-a (r/cursor !core [:click-count :a])
             :!click-count-b (r/cursor !core [:click-count :b]))))

  (stop [component] component))
