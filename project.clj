(defproject cpv-to-cpv "0.1.0-SNAPSHOT"
  :description "Discovery of associative links in Common Procurement Vocabulary"
  :url "http://github.com/jindrichmynarz/cpv-to-cpv"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/math.combinatorics "0.0.8"]
                 [clj-fuzzy "0.1.8"]
                 [org.apache.jena/jena-arq "2.12.1"]
                 [org.apache.jena/jena-core "2.12.1"]
                 [org.apache.jena/jena-tdb "1.1.1"]]
  :main cpv-to-cpv.core
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-Xmx4g"])
