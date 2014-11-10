(defproject browserstack-clj "0.1.1-SNAPSHOT"
  :description "A Clojure wrapper over the browserstack API."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1" :exclusions [crouton
                                                org.clojure/tools.reader]]])
