(defproject browserstack-clj "0.1.4-SNAPSHOT"
  :description "A Clojure wrapper over the browserstack API."
  :url         "https://github.com/yohan-pereira/browserstack-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1" :exclusions [crouton
                                                org.clojure/tools.reader]]])
