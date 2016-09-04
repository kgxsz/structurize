(ns structurize.components.image
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


;; model ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->+loaded? [+image]
  (l/*> +image (in [:loaded?])))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn image [φ {:keys [+image src]}]
  {:pre [(s/valid? ::t/lens +image)
         (s/valid? str #_::t/url src)]}
  (log-debug φ "mount image")
  (r/create-class
   {:component-did-mount #(side-effect! φ :image/did-mount {:+image +image :src src})
    :reagent-render
    (fn []
      (let [loaded? (track φ l/view-single (->+loaded? +image))]
        (log-debug φ "render image")
        [:img.l-cell.l-cell--fill-parent.c-image
         {:class (u/->class {:c-image--transparent (not loaded?)})
          :src src}]))}))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :image/did-mount
  [Φ id {:keys [+image src] :as props}]
  (let [image (js/Image.)]
    (write! Φ :image/did-mount
            (fn [x]
              (l/update x (->+loaded? +image) (l/put false))))
    (set! (.-onload image) (fn []
                             (write! Φ :image/loaded
                                     (fn [x]
                                       (l/update x (->+loaded? +image) (l/put true))))))
    (set! (.-src image) src)))
