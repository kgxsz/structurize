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

;; d3 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-texture [Φ {:keys [node]}]
  (let [svg (d3.select node)
        colour (rand-nth
                (vals (select-keys (-> vars :color) [:cadetblue
                                                     :brown
                                                     :crimson

                                                     :darkcyan
                                                     :darkmagenta
                                                     :darkslateblue
                                                     :forestgreen
                                                     :lavendar
                                                     :lightpink
                                                     :mediumslateblue
                                                     :peru
                                                     :sandybrown
                                                     :thistle

                                                     ])))
        orientation "6/8"
        width 1
        size 5
        sel (d3.select node)
        texture (doto (textures.lines)
            (.size size)
            (.strokeWidth width)
            (.orientation orientation)
            (.stroke colour))]
    (-> svg
        (.call texture)
        (.append "rect")
        (.attr "x" 0)
        (.attr "y" 0)
        (.attr "width" "100%")
        (.attr "height" "100%")
        (.style "fill" (.url texture)))))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pod [Φ]
  (log-debug Φ "render pod")
  (r/create-class
   {:component-did-mount #(make-texture Φ {:node (r/dom-node %)})
    :reagent-render (fn []
                      [:svg.l-cell.l-cell--width-100 {:style {:height (+ 200 (rand 200))}}])}))
