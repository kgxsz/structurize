(ns structurize.components.general
  (:require [structurize.system.utils :refer [side-effect! track]]
            [traversy.lens :as l])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

(defn +slide-over-open? [+slide-over]
  (l/*> +slide-over (l/in [:open?])))

(defn toggle-slide-over! [x +slide-over]
  (l/update x (+slide-over-open? +slide-over) not))

(defn slide-over
  [φ {:keys [+slide-over absolute-width direction]} content]
  (let [open? (track φ l/view-single (+slide-over-open? +slide-over))]
    (log-debug φ "render slide-over")
    [:div.l-slide-over {:style {:width (str absolute-width "px")
                                direction (if open? 0 (str (- absolute-width) "px"))}}
     content]))
