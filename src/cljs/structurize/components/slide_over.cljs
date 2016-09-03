(ns structurize.components.slide-over
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

(defn ->+slide-over-open? [+slide-over]
  (l/*> +slide-over (in [:open?])))


;; exposed functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toggle-slide-over [x +slide-over]
  (l/update x (->+slide-over-open? +slide-over) not))


;; types ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::absolute-width (s/and pos? int?))
(s/def ::direction #{:top :right :bottom :left})


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn slide-over [φ {:keys [+slide-over absolute-width direction]} content]
  {:pre [(s/valid? ::t/lens +slide-over)
         (s/valid? ::absolute-width absolute-width)
         (s/valid? ::direction direction)]}
  (let [open? (track φ l/view-single (->+slide-over-open? +slide-over))]
    (log-debug φ "render slide-over")
    [:div.c-slide-over {:style {:width (str absolute-width "px")
                                direction (if open? 0 (str (- absolute-width) "px"))}}
     content]))

