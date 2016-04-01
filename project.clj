(defproject durable-atom "0.0.1"
  :description "Simple file-backed, durable atom"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "https://github.com/polygloton/durable-atom"}
  :main nil
  :aot :all
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :dev {:dependencies [[org.clojure/clojure "1.8.0"]]}})
