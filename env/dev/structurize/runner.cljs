(ns ^:figwheel-no-load structurize.runner
  (:require [structurize.render :as render]))


(defn reload! []
  (render/render-root!))


(defn ^:export start []
  (enable-console-print!)
  (render/render-root!))
