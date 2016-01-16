(ns structurize.env)

(defonce env-vars
  {:github-auth-url "https://github.com/login/oauth/authorize"
   :github-auth-client-id "c1561f31c78052ca28e2"})

(defn env
  ([kw] (env kw nil))
  ([kw default] (get env-vars kw default)))
