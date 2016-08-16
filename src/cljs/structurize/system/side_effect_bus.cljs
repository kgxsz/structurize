(ns structurize.system.side-effect-bus
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn side-effect!
  ([Φ id] (side-effect! Φ id {}))
  ([{:keys [<side-effects] :as Φ} id props]
   (go (a/>! <side-effects [Φ id props]))))


(defrecord SideEffectBus [config-opts]
  component/Lifecycle

  (start [component]
    (log/info "initialising side-effect-bus")
    (let [<side-effects (a/chan)]
      (assoc component
             :<side-effects <side-effects)))

  (stop [component] component))
