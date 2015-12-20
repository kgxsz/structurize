(ns ^:figwheel-no-load structurize.main
  (:require [structurize.core :as core]))

(defn reload! []
  (core/render-root!))

(defn ^:export start []
  (enable-console-print!)
  (core/main))
