(defproject space-alone "0.1.0-SNAPSHOT"
  :description "good old Asteroids written in ClojureScript"
  :url "http://github.com/propan/space-alone"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "space-alone"
              :source-paths ["src"]
              :compiler {
                :output-to "space_alone.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
