(defproject structurize "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.8.51"]
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
                 [camel-snake-kebab "0.3.2"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [reagent "0.6.0-alpha"]
                 [com.cemerick/url "0.1.1"]]

  :min-lein-version "2.0.0"

  :plugins [[lein-garden "0.2.6"]
            [lein-cljsbuild "1.1.3"]]

  :clean-targets ^{:protect false} ["target/"
                                    "resources/public/js/out"
                                    "resources/public/js/structurize.js"
                                    "resources/public/css/structurize.css"]

  :profiles {:dev {:source-paths ["src/clj" "env/dev"]

                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]

                   :plugins [[lein-figwheel "0.5.0-2"]]

                   :repl-options {:init-ns structurize.main
                                  :init (structurize.main/-main)
                                  :port 4000}

                   :cljsbuild {:builds [{:id "structurize"
                                         :source-paths ["src/cljs" "env/dev"]
                                         :figwheel {:on-jsload "structurize.runner/reload!"}
                                         :compiler {:output-to "resources/public/js/structurize.js"
                                                    :main "structurize.runner"
                                                    :asset-path "/js/out"
                                                    :optimizations :none
                                                    :source-map true}}]}

                   :garden {:builds [{:id "main"
                                      :source-paths ["src/clj"]
                                      :stylesheet structurize.styles.main/main
                                      :compiler {:output-to "resources/public/css/structurize.css"
                                                 :pretty-print? true}}]}

                   :figwheel {:repl false
                              :nrepl-port 5000
                              :css-dirs ["resources/public/css"]}}

             :travis {:source-paths ["src/clj" "env/prod"]

                      :aot :all

                      :main structurize.main

                      :hooks [leiningen.cljsbuild
                              leiningen.garden]

                      :cljsbuild {:builds [{:id "structurize"
                                            :source-paths ["src/cljs" "env/prod"]
                                            :compiler {:output-to "resources/public/js/structurize.js"
                                                       :main "structurize.runner"
                                                       :asset-path "/js/out"
                                                       :optimizations :advanced}}]}

                      :garden {:builds [{:id "main"
                                         :source-paths ["src/clj"]
                                         :stylesheet structurize.styles.main/main
                                         :compiler {:output-to "resources/public/css/structurize.css"
                                                    :pretty-print? false}}]}}


             :heroku {:source-paths ["src/clj" "env/prod"]

                      :aot :all

                      :main structurize.main

                      :hooks [leiningen.cljsbuild
                              leiningen.garden]

                      :cljsbuild {:builds [{:id "structurize"
                                            :source-paths ["src/cljs" "env/prod"]
                                            :compiler {:output-to "resources/public/js/structurize.js"
                                                       :main "structurize.runner"
                                                       :asset-path "/js/out"
                                                       :optimizations :advanced}}]}

                      :garden {:builds [{:id "main"
                                         :source-paths ["src/clj"]
                                         :stylesheet structurize.styles.main/main
                                         :compiler {:output-to "resources/public/css/structurize.css"
                                                    :pretty-print? true}}]}}})
