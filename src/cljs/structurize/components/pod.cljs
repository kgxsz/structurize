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


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pod [Φ]
  (log-debug Φ "mount pod")
  (r/create-class
   {:component-did-mount #(side-effect! Φ :pod/did-mount {:node (r/dom-node %)})
    :reagent-render (fn []
                      (log-debug Φ "render pod")
                      [:div {:style {:height (+ 200 (rand 200))}}])}))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :pod/did-mount [Φ id {:keys [node]}]
  (let [colour (rand-nth ["#B39EB5" "#F49AC2" "#FF6961" "#03C03C" "#AEC6CF"
                          "#836953" "#FDFD96" "#C23B22" "#DEA5A4" "#77DD77"
                          "#FFB347" "#B19CD9" "#779ECB" "#966FD6" "#CFCFC4"])
        orientation "6/8"
        width 1
        size 5
        sel (d3.select node)
        t (doto (textures.lines)
            (.size size)
            (.strokeWidth width)
            (.orientation orientation)
            (.stroke colour))
        svg (doto (.append sel "svg")
              (.style "height" "100%")
              (.style "width" "100%")
              (.call t))
        r (doto (.append svg "rect")
            (.attr "x" 0)
            (.attr "y" 0)
            (.attr "width" "100%")
            (.attr "height" "100%")
            (.style "fill" (.url t)))])) 
