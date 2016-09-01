(ns structurize.components.tooling
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.app-browser :refer [app-browser]]
            [structurize.components.writes-browser :refer [writes-browser]]
            [structurize.components.slide-over :refer [slide-over toggle-slide-over]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tooling [φ]
  (let [+slide-over (in [:tooling :tooling-slide-over])]
    (log-debug φ "mount tooling")
    (fn []
      [:div.l-overlay.l-overlay--fill-viewport
       [slide-over φ {:+slide-over +slide-over
                      :absolute-width 800
                      :direction :right}
        [:div.l-overlay__content.c-tooling
         [:div.c-tooling__handle
          {:on-click (u/without-propagation
                      #(side-effect! φ :tooling/toggle-tooling-slide-over
                                     {:+slide-over +slide-over}))}
          [:div.c-icon.c-icon--cog]]
         [:div.l-col.l-col--fill-parent
          [:div.l-col__item.c-tooling__item
           [writes-browser φ]]
          [:div.l-col__item.l-col__item--grow.c-tooling__item
           [app-browser φ]]]]]])))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :tooling/toggle-tooling-slide-over
  [Φ id {:keys [+slide-over] :as props}]
  (write! Φ :tooling/toggle-tooling-slide-over
         (fn [x]
           (toggle-slide-over x +slide-over))))
