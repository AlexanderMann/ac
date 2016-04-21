(ns me.alxmnn.ac
  "Kicks off functions in a separate process."
  ; we need this to generate a class so that we can find the jar path
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.lang ProcessBuilder$Redirect]))

(def java-executable
  (str (System/getProperty "java.home")
       "/bin/java"))

(defn- search-for-uberjar
  "Looks in the specified path for the first jar which matches the regex.
  Throws otherwise"
  [path jar-regex]
  (let [f (io/file path)
        match?-fn (fn [x] (re-matches jar-regex (.getName x)))
        first-jar (->> f
                       file-seq
                       (filter match?-fn)
                       first)]
    (when-not first-jar
      (throw (ex-info (str "No jars found in initial path: " path)
                      {:path path
                       :jar-regex jar-regex})))
    (.getAbsolutePath first-jar)))

(defn- uberjar-path
  [uberjar-search-path jar-regex]
  (if uberjar-search-path
    (search-for-uberjar uberjar-search-path jar-regex)
    (when-let [security-codesource
               (-> uberjar-path
                   class
                   .getProtectionDomain
                   .getCodeSource
                   .getLocation)]
      (.getPath security-codesource))))

(defn- classpath
  [uberjar-search-path jar-regex]
  (str (System/getProperty "java.class.path")
       (when-let [uberjar (uberjar-path uberjar-search-path jar-regex)]
         (str ":" uberjar))))

(defn- create-standalone-process-args
  [target-fn & args]
  (->> (concat
         [java-executable
          ;"-Xms1000"
          ;"-Xmx1000"
          ;"-Duser.timezone=GMT"
          "-cp"
          ; TODO: allow users to pass in values to classpath
          (classpath nil nil)
          "clojure.main"
          "--main"
          "me.alxmnn.ac"
          target-fn]
         args)
       (map str)
       (into-array String)))

(defn- set-env-args
  "Because of the way that environ works, we have to inject Environment
  Variables THEN System Properties into our built process's environment.
  Relevant code is here: https://github.com/weavejester/environ/blob/master/environ/src/environ/core.clj#L39"
  [process-builder]
  (let [env (.environment process-builder)
        env-put (fn [[k v]] (.put env k v))]
    (doseq [kv (System/getenv)]
      (env-put kv))
    (doseq [kv (System/getProperties)]
      (env-put kv)))
  process-builder)

(defn create-standalone-process
  "Given a fully qualified symbol for the fn to be invoked, and the
  args to be passed as strings to the fn, spin up "
  [fully-qualified-fn-symbol & args]
  (-> (apply create-standalone-process-args
             fully-qualified-fn-symbol
             args)
      ProcessBuilder.
      (.redirectInput ProcessBuilder$Redirect/INHERIT)
      (.redirectOutput ProcessBuilder$Redirect/INHERIT)
      set-env-args
      .start))

(defn complete?
  "Returns the exit value when a process has exited. nil otherwise."
  [process]
  (try
    (.exitValue process)
    (catch IllegalThreadStateException e)))

(defn wait-for-exit
  "Blocks execution till the process has exited. Returns the
  exit value after a process has exited. nil otherwise."
  [process]
  (try
    (.waitFor process)
    (catch InterruptedException e)))

(defn terminate
  "Immediately terminate the process."
  [process]
  (.destroy process))

(defn error-message
  "Retrieves the error stream from the process as a human readable message."
  [process]
  (with-open [rdr (io/reader (.getErrorStream process))]
    (->> rdr
         line-seq
         (take-last 500)
         (string/join "\n"))))

(defn -main
  [fully-qualified-fn-symbol & args]
  (-> fully-qualified-fn-symbol
      symbol
      namespace
      symbol
      require)
  (let [target-fn (-> fully-qualified-fn-symbol
                      symbol
                      resolve)]
    (apply target-fn args))
  (System/exit 0))
