(ns structurize.routes)


(defonce routes
  ["/" [["" :home]
        ["auth/github" :auth-with-github]
        [true :unknown]]])

