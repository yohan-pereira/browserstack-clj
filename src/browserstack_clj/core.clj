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
  [{:keys [username access-key]} & [worker-id]]
  ((client/get (if (some? worker-id) 
                 (gen-url "worker" worker-id) ;get status of specified worker
                 (gen-url "worker"))  ;get status of all workers
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
  (str
    ;replace spaces with underscores
    (string/replace
      (if (nil? (:device browser))
        ;desktop
        (string/join "_" [(:os browser)
                          (:os_version browser)
                          (:browser browser)
                          (:browser_version browser)])
        ;mobile
        (string/join "_" [(:device browser)
                          (:os browser)
                          (:os_version browser)
                          (:browser browser)]))
      #" "
      "_")

    ;API documentation says its a png but its a jpg.
    "_" (quot (System/currentTimeMillis) 1000) ".jpg"))

(defn save-worker-screenshot!
  "saves a screenshot of the specified worker to destination."
  [{:keys [username access-key]} worker-id destination]
  (let [response (client/get (gen-url "worker" worker-id "screenshot.png") 
                             {:basic-auth   [username access-key]
                              :as           :byte-array})]
    (with-open [w (io/output-stream destination)]
      (.write w (:body response)))))

(defn wait-for-worker
  "Waits for a browser stack instance to change its status to 'running'.
  If it takes more than the timeout specified an exception is thrown else 
  the number of tries are returned."
  [creds worker-id timeout]
  (let [end (+ (* timeout 1000) (System/currentTimeMillis))] 
    (loop [tries 1]
      (when (> (System/currentTimeMillis) end)
        (throw (new Exception "Timed out waiting for BrowserStack instance to start.")))
      (if (-> (status creds worker-id)
              :status
              (= "running"))
        ;retrun if status is running
        tries

        ;else sleep for 1 second and retry
        (do 
          (println "worker not running retrying.")
          (Thread/sleep 500)
          (recur (inc tries)))))))

(defn save-url-screenshot! 
  "creates a worker for the given url and browser, saves a screenshot and then deletes the 
  worker.
  Note: workers are automatically deleted in case of any exceptions."
  [creds browser url destination & [timeout]]
  (let [worker-id (create-worker! creds browser url)]
    (try 
      (println "created worker" worker-id)
      (wait-for-worker creds worker-id (or timeout 5))
      (println "worker is running, saving screenshot.")
      (save-worker-screenshot! creds worker-id destination)
      (catch Exception e (throw e))
      (finally (do (println "deleting worker" worker-id) 
                   (delete-worker! creds worker-id))))))

(defn api-status
  "Gets the api status."
  [{:keys [username access-key]}]
  ((client/get (gen-url "status")
               {:basic-auth   [username access-key]
                :as           :json})
   :body))
