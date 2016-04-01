(ns durable-atom.core
  (:require [clojure.java.io :as io])
  (:import clojure.lang.IAtom
           clojure.lang.IDeref
           clojure.lang.IRecord
           java.io.Writer))

(defprotocol IDurable
  (write-to-disk [this])
  (read-from-disk [this]))

(defrecord DurableAtom [delegated-atom file-agent]
  IAtom
  (swap [this f]
    (let [result (.swap delegated-atom f)]
      (.write-to-disk this)
      result))
  (swap [this f arg]
    (let [result (.swap delegated-atom f arg)]
      (.write-to-disk this)
      result))
  (swap [this f arg1 arg2]
    (let [result (.swap delegated-atom f arg1 arg2)]
      (.write-to-disk this)
      result))
  (swap [this f x y args]
    (let [result (.swap delegated-atom f x y args)]
      (.write-to-disk this)
      result))
  (compareAndSet [this oldv newv]
    (let [result (.compareAndSet delegated-atom oldv newv)]
      (when result
        (.write-to-disk this))
      result))
  (reset [this newval]
    (let [result (.reset delegated-atom newval)]
      (.write-to-disk this)
      result))

  IDeref
  (deref [_]
    @delegated-atom)

  IDurable
  (write-to-disk [this]
    (send-off file-agent
              (fn [file-name]
                (let [tmp-file-name (str file-name ".tmp")]
                  (spit tmp-file-name (prn-str (.deref this)))
                  (.renameTo (io/file tmp-file-name) (io/file file-name)))
                file-name)))
  (read-from-disk [this]
    (let [file-path @file-agent]
      (.reset this (if (.canRead (io/file file-path))
                     (try
                       (read-string (slurp file-path))
                       (catch RuntimeException _ nil))
                    nil)))))

(defmethod print-method DurableAtom [x, ^Writer w]
  ((get-method print-method IRecord) x w))

(defn durable-atom
  [file-name]
  (assert (.canWrite (io/file file-name))
          (str "Can not write to " file-name))
  (doto (->DurableAtom (atom nil) (agent file-name))
    (.read-from-disk)))
