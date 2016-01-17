(ns structurize.render
  (:require [structurize.render.root-component :refer [root-controller]]
            [reagent.core :as r]))

(defn render-root! []
  (r/render [root-controller] (js/document.getElementById "root")))
