(ns structurize.components.general
  (:require [structurize.system.utils :refer [side-effect! track]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; slide-over ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn +slide-over-open? [+slide-over]
  (l/*> +slide-over (in [:open?])))

(defn toggle-slide-over! [x +slide-over]
  (l/update x (+slide-over-open? +slide-over) not))

(defn slide-over [φ {:keys [+slide-over absolute-width direction]} content]
  (let [open? (track φ l/view-single (+slide-over-open? +slide-over))]
    (log-debug φ "render slide-over")
    [:div.l-slide-over {:style {:width (str absolute-width "px")
                                direction (if open? 0 (str (- absolute-width) "px"))}}
     content]))


;; image ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn image [φ {:keys [+image src]}]
  (r/create-class
   {:component-did-mount #(side-effect! φ :image/did-mount {:+image +image :src src})
    :reagent-render
    (fn []
      (let [loaded? (track φ l/view-single (l/*> +image (in [:loaded?])))]
        [:img.l-cell.l-cell--fill-parent.c-image
         {:class (u/->class {:c-image--transparent (not loaded?)})
          :src src}]))}))
