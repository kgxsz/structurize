(ns structurize.components.triptych
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
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


(def calculate-triptych-facts
  (memoize
   (fn [{:keys [width breakpoint]}]
     (let [gutter {:xs 6
                   :sm 6
                   :md 10
                   :lg 10
                   :xl 10}
           max-col-width 220
           min-col-width 190
           min-col-n 2
           gutter (get gutter breakpoint)
           col-n (max min-col-n (quot (- width gutter) (+ min-col-width gutter)))
           col-width (min max-col-width (int (/ (- width (* (inc col-n) gutter)) col-n)))
           margin (- width (* col-n (+ col-width gutter)) gutter)]

       {:col-n col-n
        :col-width col-width
        :margin margin
        :gutter gutter}))))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn triptych [φ {:keys [center left right] :as props}]
  (let [{:keys [width breakpoint]} (track φ l/view-single
                                          (in [:viewport]))
        {:keys [col-n col-width gutter margin] :as triptych-facts} (calculate-triptych-facts {:width width :breakpoint breakpoint})
        {[center-f center-props] :c center-hidden :hidden} center
        {[left-f left-props] :c left-hidden :hidden} left
        {[right-f right-props] :c right-hidden :hidden} right
        visible? (fn [{:keys [c hidden]}]
                   (and (not (contains? hidden breakpoint))
                        (not (nil? c))))]

    (log-debug φ "render triptych")
    [:div.l-row.l-row--height-100
     (when (visible? left)
       [left-f φ (merge left-props triptych-facts
                        {:width (+ gutter col-width)
                         :col-n 1
                         :margin-left (/ margin 2)
                         :margin-right 0})])
     (when (visible? center)
       (let [left-visible? (visible? left)
             right-visible? (visible? right)]
         [center-f φ (merge center-props triptych-facts
                            {:width (cond-> (+ (* col-n col-width) (* (inc col-n) gutter))
                                      left-visible? (- (+ gutter col-width))
                                      right-visible? (- (+ gutter col-width)))
                             :col-n (cond-> col-n
                                      left-visible? dec
                                      right-visible? dec)
                             :margin-left (if left-visible? 0 (/ margin 2))
                             :margin-right (if right-visible? 0 (/ margin 2))})]))
     (when (visible? right)
       [right-f φ (merge right-props triptych-facts
                         {:width (+ gutter col-width)
                          :col-n 1
                          :margin-left 0
                          :margin-right (/ margin 2)})])]))
