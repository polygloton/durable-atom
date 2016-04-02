(ns durable-atom.core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [durable-atom.core :refer [durable-atom]])
  (:import clojure.lang.ExceptionInfo
           java.io.File))

(defonce test-file (atom nil))

(defn get-file-path []
  (.getAbsolutePath ^File @test-file))

(defn read-test-file []
  (slurp @test-file))

(defn write-test-file [s]
  (spit @test-file s))

(use-fixtures :once
  (fn [test]
    (reset! test-file
            (doto (File/createTempFile "test-durable-atom" ".edn")
              (.deleteOnExit)))
    (test)))

(use-fixtures :each
  (fn [test]
    (write-test-file "")
    (test)))

(deftest durable-atom-reset!-swap!-test
  (let [a (durable-atom (get-file-path))]
    (is (= "" (read-test-file)))
    (reset! a {:foo "bar"})
    (await (:file-agent a))
    (is (= "{:foo \"bar\"}\n" (read-test-file)))
    (is (= {:foo "bar"} @a))
    (swap! a #(assoc % :foo "swap1"))
    (await (:file-agent a))
    (is (= "{:foo \"swap1\"}\n" (read-test-file)))
    (is (= {:foo "swap1"} @a))
    (swap! a #(assoc %1 :foo %2) "swap2")
    (await (:file-agent a))
    (is (= "{:foo \"swap2\"}\n" (read-test-file)))
    (is (= {:foo "swap2"} @a))
    (swap! a #(assoc %1 %2 %3) :foo "swap3")
    (await (:file-agent a))
    (is (= "{:foo \"swap3\"}\n" (read-test-file)))
    (is (= {:foo "swap3"} @a))
    (swap! a assoc :foo "swap4a" :bar "swap4b")
    (await (:file-agent a))
    (is (= "{:foo \"swap4a\", :bar \"swap4b\"}\n" (read-test-file)))
    (is (= {:foo "swap4a" :bar "swap4b"} @a))))

(deftest durable-atom-reopen-test
  (let [a (durable-atom (get-file-path))]
    (is (= "" (read-test-file)))
    (reset! a {:foo "bar"})
    (await (:file-agent a)))
  (let [b (durable-atom (get-file-path))]
    (is (= "{:foo \"bar\"}\n" (read-test-file)))
    (is (= {:foo "bar"} @b))
    (reset! b {:foo "baz"})
    (await (:file-agent b))
    (is (= "{:foo \"baz\"}\n" (read-test-file)))
    (is (= {:foo "baz"} @b))))

(deftest durable-atom-compare-and-set-test
  (let [a (durable-atom (get-file-path))]
    (is (nil? @a))
    (reset! a 0)
    (is (= 0 @a))
    (is (true? (compare-and-set! a 0 10)))
    (is (false? (compare-and-set! a 20 30)))
    (await (:file-agent a))
    (is (= "10\n" (read-test-file)))
    (is (= 10 @a))))

(deftest durable-atom-corrupt-file-test
  (write-test-file "^%$#%^")
  (is (thrown? ExceptionInfo (durable-atom (get-file-path))))
  (write-test-file ""))
