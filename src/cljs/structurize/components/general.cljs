(ns structurize.components.general
  (:require [structurize.system.utils :refer [side-effect! track]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; slide-over ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn +slide-over-open? [+slide-over]
  (l/*> +slide-over (in [:open?])))


(defn toggle-slide-over! [x +slide-over]
  (l/update x (+slide-over-open? +slide-over) not))


(defn slide-over [φ {:keys [+slide-over absolute-width direction]} content]
  {:pre [(s/valid? ::t/lens +slide-over)
         (s/valid? (s/and pos? int?) absolute-width)
         (s/valid? #{:top :right :bottom :left} direction)]}
  (let [open? (track φ l/view-single (+slide-over-open? +slide-over))]
    (log-debug φ "render slide-over")
    [:div.l-slide-over {:style {:width (str absolute-width "px")
                                direction (if open? 0 (str (- absolute-width) "px"))}}
     content]))


;; image ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn image [φ {:keys [+image src]}]
  {:pre [(s/valid? ::t/lens +image)
         (s/valid? ::t/url src)]}
  (log-debug φ "mount image")
  (r/create-class
   {:component-did-mount #(side-effect! φ :image/did-mount {:+image +image :src src})
    :reagent-render
    (fn []
      (let [loaded? (track φ l/view-single (l/*> +image (in [:loaded?])))]
        (log-debug φ "render image")
        [:img.l-cell.l-cell--fill-parent.c-image
         {:class (u/->class {:c-image--transparent (not loaded?)})
          :src src}]))}))
