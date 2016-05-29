(ns structurize.system.side-effect-bus
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defrecord SideEffectBus [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effect-bus")
    (let [<side-effects (a/chan)]

      (assoc component
             :<side-effects <side-effects
             :emit-side-effect (fn [side-effect] (go (a/>! <side-effects side-effect))))))

  (stop [component] component))
