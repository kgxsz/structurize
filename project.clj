(defproject structurize "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [environ "1.0.1"]
                 [bidi "1.22.1"]
                 [http-kit "2.1.18"]
                 [hiccup "1.0.5"]
                 [ring "1.4.0"]
                 [com.taoensso/sente "1.7.0"]
                 [com.taoensso/timbre "4.1.4"]]

  :plugins [[lein-environ "1.0.1"]]

  :profiles {:dev {:source-paths ["env/dev"]

                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]

                   :repl-options {:init-ns structurize.main
                                  :init (structurize.main/start!)
                                  :port 4000}

                   :env {:port 3000}}})
