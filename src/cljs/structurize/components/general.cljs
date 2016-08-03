(ns structurize.components.general
  (:require [taoensso.timbre :as log]))


(defn slide-over
  ;; TODO - work on the docs and fleshing this guys out ..  maybe some spec?
  "blah blah blah"
  [φ {:keys [open? absolute-width direction]} content]

  (log/debug "render slide-over")
  [:div.l-slide-over {:style {:width (str absolute-width "px")
                              direction (if open? 0 (str (- absolute-width) "px"))}}
   content])
