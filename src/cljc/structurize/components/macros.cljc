(ns structurize.components.macros
  #?(:clj (:require [taoensso.timbre :refer [info debug error]])))

#?(:clj (defmacro log [φ level body]
          `(when (or (not (:tooling? (meta ~φ)))
                     (get-in ~φ [:config-opts :tooling :log?]))
             (~level ~@body))))

#?(:clj (defmacro log-info [φ & body] `(log ~φ info ~body)))
#?(:clj (defmacro log-debug [φ & body] `(log ~φ debug ~body)))
#?(:clj (defmacro log-error [φ & body] `(log ~φ error ~body)))
