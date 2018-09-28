(defproject proto-repl-client-cljs "0.0.1-SNAPSHOT"
  :description "Implementations of portions of Proto REPL in clojurescript."
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [fipp "0.6.12"]
                 [replumb "0.2.4"]
                 [proto-repl-charts "0.3.2"]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {:builds [{:source-paths ["src-cljs" "src"]
                        :compiler {:output-to "../lib/proto_repl/edn_reader.js"
                                   :main proto-repl.edn-reader
                                   :optimizations :simple
                                   :output-wrapper true
                                   :target :nodejs}}
                       {:source-paths ["src-cljs" "src"]
                        :compiler {:output-to "../lib/proto_repl/self_hosted.js"
                                   :main proto-repl.self-hosted
                                   :optimizations :simple
                                   :output-wrapper true
                                   :target :nodejs}}]}

  :profiles
  {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                        [pjstadig/humane-test-output "0.8.3"]
                        [proto-repl "0.3.2"]]
         :injections [(require 'pjstadig.humane-test-output)
                      (pjstadig.humane-test-output/activate!)]
         :source-paths ["dev" "src" "tests"]}})
