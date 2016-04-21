(ns me.alxmnn.ac-test
  (:require [clojure.string :as s]
            [clojure.test :refer :all]
            [me.alxmnn.ac :as ac]))

(def test-path (str "/tmp/me.alxmnn.ac-test-"
                    (System/currentTimeMillis)))

(defn put-in
  [file-path some-val]
  (spit file-path some-val))

(defn get-out
  [file-path]
  (slurp file-path))

(defn pause-indefinitely
  []
  (Thread/sleep 1234567890))

(defn blow-up
  []
  (apply str (repeat "...and beyond")))

(deftest about-create-standalone-process
  (testing "you get-out what you put-in"
    (let [expected "test"
          process (ac/create-standalone-process
                    'me.alxmnn.ac-test/put-in
                    test-path
                    expected)]
      (ac/wait-for-exit process)
      (is (= (get-out test-path)
             expected))))
  (testing "terminate"
    (let [process (ac/create-standalone-process
                    'me.alxmnn.ac-test/pause-indefinitely)]
      (is (nil? (ac/complete? process)))
      (ac/terminate process)
      (ac/wait-for-exit process)
      (is (not= (ac/complete? process)
                0))))
  (testing "insulates root jvm from OOM issues"
    (let [process (ac/create-standalone-process
                    'me.alxmnn.ac-test/blow-up)]
      (ac/wait-for-exit process)
      (is (not= (ac/complete? process)
                0))
      (is (->> (ac/error-message process)
               s/split-lines
               (map (partial re-matches #".*OutOfMemoryError.*Java heap space.*"))
               (some identity))))))

(comment
  (run-tests))
