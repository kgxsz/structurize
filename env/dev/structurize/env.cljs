(ns structurize.env)

(defonce env-vars
  {:something "not default"})

(defn env
  ([kw] (env kw nil))
  ([kw default] (get env-vars kw default)))
