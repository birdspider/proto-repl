(defproject proto-repl "0.3.2"
  :description "A set of helper functions for projects used in Proto REPL"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.namespace "0.2.11"]
                 ;; For completions
                 [compliment "0.3.6"]]

  :profiles
  {:dev {:dependencies [[pjstadig/humane-test-output "0.8.3"]]
         :injections [(require 'pjstadig.humane-test-output)
                      (pjstadig.humane-test-output/activate!)]
         :source-paths ["dev" "src" "test"]}})
