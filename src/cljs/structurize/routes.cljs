(ns structurize.routes)


(defonce routes
  ["/" {"" :home
        "auth/github" :auth-github
        "foo" :foo
        "bar" :bar}])

