(ns structurize.routes)


(defonce routes
  ["/" [["" :home]
        ["sign-in/github" :sign-in-with-github]
        [true :unknown]]])

