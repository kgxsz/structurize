(defproject structurize "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.227"]
                 [org.clojure/core.async "0.2.385"]
                 [com.stuartsierra/component "0.3.1"]
                 [bidi "1.25.0"]
                 [http-kit "2.1.18"]
                 [hiccup "1.0.5"]
                 [ring "1.4.0"]
                 [medley "0.7.1"]
                 [ring/ring-defaults "0.1.5"]
                 [com.taoensso/sente "1.10.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [traversy "0.4.0"]
                 [clj-time "0.11.0"]
                 [garden "1.3.2"]
                 [cljsjs/lodash "4.11.2-0"]
                 [cljsjs/d3 "3.5.16-0"]
                 [cljsjs/textures "1.0.3-0"]
                 [camel-snake-kebab "0.3.2"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [reagent "0.6.0-alpha"]
                 [com.cemerick/url "0.1.1"]]

  :min-lein-version "2.0.0"

  :clean-targets ^{:protect false} ["target/"
                                    "resources/public/js/out"
                                    "resources/public/js/structurize.js"
                                    "resources/public/css/structurize.css"]

  :plugins [[lein-garden "0.2.6"]]

  :uberjar-name "structurize-standalone.jar"

  :profiles {:dev {:source-paths ["src/clj" "src/cljc" "env/dev/clj"]

                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]

                   :plugins [[lein-figwheel "0.5.6"]]

                   :repl-options {:init-ns structurize.main
                                  :init (structurize.main/start!)
                                  :port 4000}

                   :cljsbuild {:builds [{:id "structurize"
                                         :source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                                         :figwheel {:on-jsload "structurize.runner/reload"}
                                         :compiler {:output-to "resources/public/js/structurize.js"
                                                    :main "structurize.runner"
                                                    :asset-path "/js/out"
                                                    :foreign-libs [{:file "dev-resources/js/foreign-libs/d3-voronoi.js"
                                                                    :provides ["d3.voronoi"]}]
                                                    :optimizations :none
                                                    :source-map true}}]}

                   :garden {:builds [{:id "main"
                                      :source-paths ["src/cljc"]
                                      :stylesheet structurize.styles.main/main
                                      :compiler {:output-to "resources/public/css/structurize.css"
                                                 :pretty-print? true}}]}

                   :figwheel {:repl false
                              :nrepl-port 5000
                              :css-dirs ["resources/public/css"]}}

             :uberjar {:source-paths ["src/clj" "src/cljc" "env/prod/clj"]

                       :aot :all

                       :main structurize.main

                       :plugins  [[lein-cljsbuild "1.1.3"]]

                       :hooks [leiningen.cljsbuild
                               leiningen.garden]

                       :cljsbuild {:builds [{:id "structurize"
                                             :source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
                                             :compiler {:output-to "resources/public/js/structurize.js"
                                                        :main "structurize.runner"
                                                        :asset-path "/js/out"
                                                        :foreign-libs [{:file  "dev-resources/js/foreign-libs/d3-voronoi.js"
                                                                        :provides ["d3.voronoi"]}]
                                                        :optimizations :advanced}}]}

                       :garden {:builds [{:id "main"
                                          :source-paths ["src/cljc"]
                                          :stylesheet structurize.styles.main/main
                                          :compiler {:output-to "resources/public/css/structurize.css"
                                                     :pretty-print? false}}]}}})
