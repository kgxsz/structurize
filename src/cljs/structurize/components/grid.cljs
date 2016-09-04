(ns structurize.components.grid
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [medley.core :as m]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


(defn visible? [{:keys [c hidden]} {:keys [width breakpoint]}]
  (and (not (contains? hidden breakpoint))
       (not (nil? c))))

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn grid-column [Φ {:keys [width gutter cs]}]
  (log-debug Φ "render grid column")
  [:div {:style {:width width}}
   (doall
    (for [[i c] (m/indexed cs)]
      [:div {:key i
             :style {:margin-top gutter}}
       [c Φ]]))])

(defn grid [φ {:keys [center left right top bottom] :as props}]
  (let [{:keys [width grid] :as viewport} (track φ l/view-single
                                            (in [:viewport]))
        {:keys [col-n col-width gutter margin]} grid
        {center-c :c center-hidden :hidden} center
        {left-c :c left-hidden :hidden} left
        {right-c :c right-hidden :hidden} right
        {top-c :c top-hidden :hidden} top
        {bottom-c :c bottom-hidden :hidden} bottom]

    (log-debug φ "render grid")

    [:div
     (when (visible? top viewport)
       [top-c φ (assoc grid
                       :width (- width margin)
                       :margin-left (/ margin 2)
                       :margin-right (/ margin 2))])
     [:div.l-row
      (when (visible? left viewport)
        [left-c φ (assoc grid
                         :width (+ gutter col-width)
                         :col-n 1
                         :margin-left (/ margin 2)
                         :margin-right 0)])
      (when (visible? center viewport)
        (let [left-visible? (visible? left viewport)
              right-visible? (visible? right viewport)]
          [center-c φ (assoc grid
                             :width (cond-> (+ (* col-n col-width) (* (inc col-n) gutter))
                                      left-visible? (- (+ gutter col-width))
                                      right-visible? (- (+ gutter col-width)))
                             :col-n (cond-> col-n
                                      left-visible? dec
                                      right-visible? dec)
                             :margin-left (if left-visible? 0 (/ margin 2))
                             :margin-right (if right-visible? 0 (/ margin 2)))]))
      (when (visible? right viewport)
        [right-c φ (assoc grid
                          :width (+ gutter col-width)
                          :col-n 1
                          :margin-left (/ margin 2)
                          :margin-right 0)])]
     (when (visible? bottom viewport)
       [bottom-c φ (assoc grid
                          :width (- width margin)
                          :margin-left (/ margin 2)
                          :margin-right (/ margin 2))])]))
