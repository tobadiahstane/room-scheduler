(defproject room-scheduler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]

                ;web serving
                 [ring "1.8.1"]
                 [compojure "1.6.1"]]

  :main ^:skip-aot room-scheduler.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[proto-repl "0.3.1"]]}})
