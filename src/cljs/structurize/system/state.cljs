(ns structurize.system.state
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defn make-global-state [{:keys [general]}]
  (r/atom {:click-count {:a (:init-click-count-a general)
                         :b 0}}))


(defrecord State [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "Initialising state")
    (let [!global (make-global-state config-opts)]
      (assoc component
             :!global !global
             :!click-count-a (r/cursor !global [:click-count :a])
             :!click-count-b (r/cursor !global [:click-count :b]))))

  (stop [component] component))
