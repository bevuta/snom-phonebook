(defproject snom-phonebook "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [mysql/mysql-connector-java "5.1.35"]
                 [org.immutant/web "2.0.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.clojure/data.xml "0.0.8"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler snom-phonebook.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}
   :uberjar {:aot :all}}
  :main snom-phonebook.handler)
