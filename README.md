# browserstack-clj


A Clojure library wrapping the [[https://github.com/browserstack/api][BrowserStack API]]. 

## Installation

** Installation

=browserstack-clj= is available as a Maven artifact from [[https://clojars.org/browserstack-clj][Clojars]]:

[![Clojars Project](http://clojars.org/browserstack-clj/latest-version.svg)](http://clojars.org/browserstack-clj)

## Usage

First, require it in the REPL:

#+BEGIN_SRC clojure
(require '[browserstack-clj.core :as browserstack])
#+END_SRC

Or in your application:

#+BEGIN_SRC clojure
(ns my-app.core
  (:require [browserstack-clj.core :as browserstack]))
#+END_SRC


Heres a sample program that saves screenshots of a url on all versions
of IE supported on windows 7.

#+BEGIN_SRC clojure

(ns browserstack-clj-sample.core
  (:require [browserstack-clj.core :refer [browsers gen-filename save-url-screenshot!])
  (:gen-class))


(defn -main
  [& args]
  (let [creds {:username "username" :access-key "browserstack-accesskey"}
	;filter matching browsers
        ies       (filter #(and (= "Windows" (:os         %))
                                (= "7"       (:os_version %))  
                                (= "ie"      (:browser    %))) 
                          (browsers creds))]
    (doseq [browser ies]
      ;generate a filename for the screenshots using the helper function.
      (let [filename (str "./screenshots/" (gen-filename browser))]
        (println "saving to" filename)
	;Note: workers are automatically deleted in case of any exceptions.
        (save-url-screenshot! creds browser "https://github.com/404" filename)))))
#+END_SRC

Additionally you can access various API functions like create-worker!, status, delete-worker!
and save-worker-screenshot! individually if you need to. Refer to the inline documentation for
their signatures.



## License

Copyright © 2014 Yohan Pereira

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.