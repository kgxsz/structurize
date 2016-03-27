(defproject structurize "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [com.stuartsierra/component "0.3.1"]
                 [bidi "1.25.0"]
                 [http-kit "2.1.18"]
                 [hiccup "1.0.5"]
                 [ring "1.4.0"]
                 [medley "0.7.1"]
                 [ring/ring-defaults "0.1.5"]
                 [com.taoensso/sente "1.8.0-beta1"]
                 [com.taoensso/timbre "4.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.11.0"]
                 [garden "1.3.2"]
                 [camel-snake-kebab "0.3.2"]]

  :clean-targets ^{:protect false} ["target/"
                                    "dev-resources/public/js/"
                                    "dev-resources/public/css/style.css"]

  :profiles {:dev {:source-paths ["src/clj" "env/dev"]

                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [com.andrewmcveigh/cljs-time "0.4.0"]
                                  [reagent "0.5.1"]
                                  [com.cemerick/url "0.1.1"]
                                  [secretary "1.2.3"]]

                   :plugins [[lein-figwheel "0.5.0-2"]
                             [lein-garden "0.2.6"]]

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

                   :garden {:builds [{:id "main"
                                      :source-paths ["src/clj"]
                                      :stylesheet structurize.styles.main/main
                                      :compiler {:output-to "dev-resources/public/css/style.css"
                                                 :pretty-print? true}}]}

                   :figwheel {:repl false
                              :nrepl-port 5000
                              :css-dirs ["dev-resources/public/css"]}}})
