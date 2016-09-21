(ns structurize.components.triptych
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

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn triptych [φ {:keys [center left right] :as props}]
  (let [{:keys [width breakpoint triptych] :as viewport} (track φ l/view-single
                                                                (in [:viewport]))
        {:keys [col-n col-width gutter margin]} triptych
        {center-c :c center-hidden :hidden} center
        {left-c :c left-hidden :hidden} left
        {right-c :c right-hidden :hidden} right
        visible? (fn [{:keys [c hidden]}]
                   (and (not (contains? hidden breakpoint))
                        (not (nil? c))))]

    (log-debug φ "render triptych")
    [:div.l-row.l-row--height-100
     (when (visible? left)
       [left-c φ (assoc triptych
                        :width (+ gutter col-width)
                        :col-n 1
                        :margin-left (/ margin 2)
                        :margin-right 0)])
     (when (visible? center)
       (let [left-visible? (visible? left)
             right-visible? (visible? right)]
         [center-c φ (assoc triptych
                            :width (cond-> (+ (* col-n col-width) (* (inc col-n) gutter))
                                     left-visible? (- (+ gutter col-width))
                                     right-visible? (- (+ gutter col-width)))
                            :col-n (cond-> col-n
                                     left-visible? dec
                                     right-visible? dec)
                            :margin-left (if left-visible? 0 (/ margin 2))
                            :margin-right (if right-visible? 0 (/ margin 2)))]))
     (when (visible? right)
       [right-c φ (assoc triptych
                         :width (+ gutter col-width)
                         :col-n 1
                         :margin-left 0
                         :margin-right (/ margin 2))])]))
