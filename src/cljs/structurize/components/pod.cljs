(ns structurize.components.pod
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.styles.vars :refer [vars]]
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

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pod [Φ]
  (log-debug Φ "render pod")
  [:div {:style {:height (+ 200 (rand 200))
                 :background-color (rand-nth
                                    (vals (select-keys (-> vars :color) [:green
                                                                         :purple
                                                                         :yellow
                                                                         :red
                                                                         :orange
                                                                         :blue])))
                 :opacity 0.1}}])
