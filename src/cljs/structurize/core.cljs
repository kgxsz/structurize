(ns structurize.core
  (:require [reagent.core :as r]
            [taoensso.timbre :as log]))

(defn root-view []
  [:div
   [:h1 "Front end ready!"]
   [:h3 "more to come.."]])

(defn render-root! []
  (r/render [root-view] (js/document.getElementById "root")))

(defn main []
  (render-root!))
