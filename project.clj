(defproject structurize "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [bidi "1.22.1"]
                 [http-kit "2.1.18"]
                 [hiccup "1.0.5"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [com.taoensso/sente "1.7.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [com.cemerick/url "0.1.1"]]

  :clean-targets ^{:protect false} ["target/"
                                    "dev-resources/public/js/"
                                    "dev-resources/public/css/"]

  :profiles {:dev {:source-paths ["src/clj" "env/dev"]

                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [reagent "0.5.1"]]

                   :plugins [[lein-figwheel "0.5.0-2"]]

                   :repl-options {:init-ns structurize.main
                                  :init (structurize.main/start!)
                                  :port 4000}

                   :cljsbuild {:builds [{:id "structurize"
                                         :source-paths ["src/cljs" "env/dev"]
                                         :figwheel {:on-jsload "structurize.runner/reload!"}
                                         :compiler {:output-dir "dev-resources/public/js"
                                                    :output-to "dev-resources/public/js/structurize.js"
                                                    :main "structurize.runner"
                                                    :asset-path "/js/"
                                                    :optimizations :none
                                                    :source-map true}}]}

                   :figwheel {:repl false
                              :nrepl-port 5000
                              :css-dirs ["dev-resources/public/css"]}}})
