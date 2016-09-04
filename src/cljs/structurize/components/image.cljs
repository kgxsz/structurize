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


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn placeholder [φ]
  (r/create-class
   {:component-did-mount #(side-effect! φ :image/placeholder-did-mount {:node (r/dom-node %)})
    :reagent-render (fn []
                      [:div.l-cell.l-cell--fill-parent.c-image__placeholder {:style {:position :absolute}}])}))


(defn image [φ {:keys [+image src size pos-x pos-y] :or {size :cover pos-x "50%" pos-y "50%"}}]
  {:pre [(s/valid? ::t/lens +image)
         (s/valid? (s/or ::t/url string?) src)]}
  (log-debug φ "mount image")
  (r/create-class
   {:component-did-mount #(side-effect! φ :image/did-mount {:node (r/dom-node %) :+image +image :src src})
    :reagent-render (fn []
                      (let [{:keys [loaded?]} (track φ l/view-single +image)]
                        (log-debug φ "render image")
                        [:div.l-cell.l-cell--fill-parent.c-image
                         [placeholder φ]
                         [:div.l-cell.l-cell--fill-parent.c-image__content {:class (u/->class {:c-image__content--transparent (not loaded?)})
                                                                            :style {:background (str "url(" src ")")
                                                                                    :background-size size
                                                                                    :background-position [pos-x pos-y]
                                                                                    :background-repeat :no-repeat}}]]))}))


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


(defmethod process-side-effect :image/placeholder-did-mount [Φ id {:keys [node] :as props}]
  (let [colour (rand-nth ["#B39EB5" "#F49AC2" "#FF6961" "#03C03C" "#AEC6CF"
                          "#836953" "#FDFD96" "#C23B22" "#DEA5A4" "#77DD77"
                          "#FFB347" "#B19CD9" "#779ECB" "#966FD6" "#CFCFC4"])
        sel (d3.select node)
        t (doto (textures.lines)
            (.size 5)
            (.strokeWidth 1)
            (.stroke colour))
        svg (doto (.append sel "svg")
              (.attr "height" "100%")
              (.attr "width" "100%")
              (.call t))
        r (doto (.append svg "rect")
            (.attr "x" 0)
            (.attr "y" 0)
            (.attr "width" "100%")
            (.attr "height" "100%")
            (.style "fill" (.url t)))]))
