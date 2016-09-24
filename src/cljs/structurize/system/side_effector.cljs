(ns structurize.system.side-effector
  (:require [bidi.bidi :as b]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            [taoensso.timbre :as log])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; side-effect handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; component setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord SideEffector []
  component/Lifecycle
  (start [component] component)
  (stop [component] component))
