(ns structurize.components.general
  (:require [taoensso.timbre :as log]
            [structurize.system.side-effect-bus :refer [side-effect!]]
            [structurize.system.state :refer [track]]
            [traversy.lens :as l]))

(defn +slide-over-open? [+slide-over]
  (l/*> +slide-over (l/in [:open?])))

(defn toggle-slide-over! [x +slide-over]
  (l/update x (+slide-over-open? +slide-over) not))

(defn slide-over
  [φ {:keys [+slide-over absolute-width direction]} content]
  (let [open? (track φ l/view-single (+slide-over-open? +slide-over))]
    (log/debug "render slide-over")
    [:div.l-slide-over {:style {:width (str absolute-width "px")
                                direction (if open? 0 (str (- absolute-width) "px"))}}
     content]))
