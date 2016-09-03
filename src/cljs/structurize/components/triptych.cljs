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


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn triptych [φ {:keys [center left right top bottom]}]
  (log-debug φ "mount triptych")
  (r/create-class
   {:component-did-mount #(side-effect! φ :triptych/did-mount)
    :reagent-render
    (fn []
      (let [breakpoint (track φ l/view-single
                              (in [:viewport :breakpoint]))
            {center-c :c} center
            {left-c :c} left
            {right-c :c} right
            {top-c :c} top
            {bottom-c :c} bottom]

        (log-debug φ "render triptych")
        (log-debug φ "Breakpoint:" breakpoint)

        [:div.c-triptych
         [top-c φ]
         [:div.l-row
          [left-c φ]
          [center-c φ]
          [right-c φ]]
         [bottom-c φ]])


      )}))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :triptych/did-mount
  [Φ id props])
