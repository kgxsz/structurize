(ns structurize.runner
  (:require [structurize.system :refer [system]]
            [cljsjs.lodash]
            [cljsjs.d3]
            [cljsjs.textures]))

(defn ^:export start []
  (enable-console-print!))
