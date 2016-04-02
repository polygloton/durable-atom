(defproject durable-atom "0.0.2"
  :description "Simple file-backed, durable atom"
  :url "https://github.com/polygloton/durable-atom"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :main nil
  :aot :all
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :dev {:dependencies [[org.clojure/clojure "1.8.0"]]}})
