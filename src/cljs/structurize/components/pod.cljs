(ns structurize.components.pod
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - don't pass anon fns
;; TODO - co-locate CSS
;; TODO - use BEM utility
;; TODO - spec everywhere

(def pastel-colours ["#B39EB5" "#F49AC2" "#FF6961" "#03C03C" "#AEC6CF"
                     "#836953" "#FDFD96" "#C23B22" "#DEA5A4" "#77DD77"
                     "#FFB347" "#B19CD9" "#779ECB" "#966FD6" "#CFCFC4"])


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pod [Φ]
  (log-debug Φ "render pod")
  [:div {:style {:height (+ 200 (rand 200))
                 :background-color (rand-nth pastel-colours)
                 :opacity 0.2}}])
