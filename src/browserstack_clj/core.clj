(ns browserstack-clj.core
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys stringify-keys]]))

(import [java.net URLEncoder])
(import [java.io File])

(def URL "http://api.browserstack.com")

(def VERSION "3")

(defn- gen-url [& elements]
  (string/join "/" (conj elements VERSION URL)))


(defn browsers
  "fetches details about the available browser targets. Pass :all to fetch all entries 
  (including betas). Retruns a list of browser definations which can be passed to functions
  that need a browser defination (eg. create-worker!)."
  [{:keys [username access-key]} & options]
  (let [options  (set options) ; convert options to a set
        params   {:flat true}

        params   (if (contains? options :all) 
                   (assoc params :all true) 
                   params)

        response (client/get (gen-url "browsers") 
                             {:query-params params
                              :basic-auth   [username access-key]
                              :as           :json})]
    (:body response)))

(defn create-worker!
  "creates a browser instance with the supplied parameters. Returns the id 
  of the newly created instance."
  [{:keys [username access-key]} browser url & [options]]
  (let [response (client/post (gen-url "worker")
                              {:query-params (assoc (merge browser options) :url url)
                               :basic-auth [username access-key]
                               :as           :json})]
    (-> response :body :id)))

(defn status
  "gets the status of a worker"
  [{:keys [username access-key]} worker-id]
  ((client/get (gen-url "worker" worker-id) 
               {:basic-auth   [username access-key]
                :as           :json})
   :body))

(defn delete-worker!
  "deletes a worker with the given id and returns the elapsed worker time."
  [{:keys [username access-key]} worker-id]
  (let [response (client/delete (gen-url "worker" worker-id)
                                {:basic-auth [username access-key]
                                 :as         :json})]
    (-> response :body :time)))


(defn gen-filename
  "Helper function to generate a file name for screenshots."
  [browser]
  ;replace spaces with underscores
  (string/replace
    (if (nil? (:device browser))
      ;desktop
      (string/join "_" [(:os browser)
                        (:os_version browser)
                        (:browser browser)
                        (:browser_version browser)
                        (quot (System/currentTimeMillis) 1000)
                        ".png"])
      ;mobile
      (string/join "_" [(:device browser)
                        (:os browser)
                        (:os_version browser)
                        (:browser browser)
                        (quot (System/currentTimeMillis) 1000)
                        ".png" 
                        ]))
    #" "
    "_"))

(defn save-worker-screenshot!
  "saves a screenshot of the specified worker to destination."
  [{:keys [username access-key]} worker-id destination]
  (let [response (client/get (gen-url "worker" worker-id "screenshot.png") 
                             {:basic-auth   [username access-key]
                              :as           :byte-array})]
    (with-open [w (io/output-stream destination)]
      (.write w (:body response)))))

(defn save-url-screenshot! 
  "creates a worker for the given url and browser, saves a screenshot and then deletes the 
  worker."
  [creds browser url destination]
  (let [worker-id (create-worker! creds browser url)]
    (try 
      (println "created worker" worker-id)
      (clojure.pprint/pprint (status creds worker-id))
      (save-worker-screenshot! creds worker-id destination)
      (catch Exception e (throw e))
      (finally (do (println "deleting worker" worker-id) 
                   (delete-worker! creds worker-id))))))

