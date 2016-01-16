(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.core :as core]))


(defn reload! []
  (core/render-root!))


(defn ^:export start []
  (enable-console-print!)
  (core/render-root!))
