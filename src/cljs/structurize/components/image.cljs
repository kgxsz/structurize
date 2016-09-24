(ns structurize.components.image
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

;; TODO - don't pass anon fns
;; TODO - co-locate CSS
;; TODO - use BEM utility
;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn image [φ {:keys [+image src size pos-x pos-y] :or {size :cover pos-x "50%" pos-y "50%"}}]
  {:pre [(s/valid? ::t/lens +image)
         (s/valid? (s/or ::t/url string?) src)]}
  (log-debug φ "mount image")
  (r/create-class
   {:component-did-mount #(side-effect! φ :image/did-mount {:node (r/dom-node %) :+image +image :src src})
    :component-will-unmount #(side-effect! φ :image/will-unmount {:+image +image})
    :reagent-render (fn []
                      (let [{:keys [loaded?]} (track φ l/view-single +image)]
                        (log-debug φ "render image")
                        [:div.l-cell.l-cell--fill-parent.c-image {:class (u/->class {:is-transparent (not loaded?)})
                                                                  :style {:background (str "url(" src ")")
                                                                          :background-size size
                                                                          :background-position [pos-x pos-y]
                                                                          :background-repeat :no-repeat}}]))}))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :image/did-mount
  [Φ id {:keys [node +image src] :as props}]
  (let [image (js/Image.)]
    (write! Φ :image/did-mount
            (fn [x]
              (l/update x +image #(assoc % :loaded? false))))
    (set! (.-onload image) (fn []
                             (write! Φ :image/loaded
                                     (fn [x]
                                       (l/update x +image #(assoc % :loaded? true))))))
    (set! (.-src image) src)))


(defmethod process-side-effect :image/will-unmount
  [Φ id {:keys [node +image src] :as props}]
  (let [image (js/Image.)]
    (write! Φ :image/will-unmount
            (fn [x]
              (l/update x +image #(assoc % :loaded? false))))))
