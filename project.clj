(defproject sebser-test-task "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]

                 [info.sunng/ring-jetty9-adapter "0.14.2"]
                 [ring/ring "1.8.1"]
                 [metosin/reitit "0.5.15"]
                 [instaparse "1.4.10"]]
  :main ^:skip-aot sebser-test-task.core
  :repl-options {:init-ns sebser-test-task.core})
