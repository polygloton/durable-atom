(ns durable-atom.core
  (:require [clojure.java.io :as io])
  (:import clojure.lang.IAtom
           clojure.lang.IDeref
           clojure.lang.IRecord
           java.io.IOException
           java.io.Writer))

(defprotocol IDurable
  (write-to-disk [this])
  (read-from-disk [this]))

(defrecord DurableAtom [delegated-atom file-agent lock]
  IAtom
  (swap [this f]
    (locking lock
      (let [result (.swap delegated-atom f)]
        (.write-to-disk this)
        result)))
  (swap [this f arg]
    (locking lock
      (let [result (.swap delegated-atom f arg)]
        (.write-to-disk this)
        result)))
  (swap [this f arg1 arg2]
    (locking lock
      (let [result (.swap delegated-atom f arg1 arg2)]
        (.write-to-disk this)
        result)))
  (swap [this f x y args]
    (locking lock
      (let [result (.swap delegated-atom f x y args)]
        (.write-to-disk this)
        result)))
  (compareAndSet [this oldv newv]
    (locking lock
      (let [result (.compareAndSet delegated-atom oldv newv)]
        (when result
          (.write-to-disk this))
        result)))
  (reset [this newval]
    (locking lock
      (let [result (.reset delegated-atom newval)]
        (.write-to-disk this)
        result)))

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
    (let [file-path @file-agent
          file (io/file file-path)]
      (.reset this
              (cond (not (.exists file))
                    (try
                      (do (spit file "")
                          nil)
                      (catch IOException exception
                        (throw
                         (ex-info "Failed writing to durable-atom file"
                                  {:file-path file-path}
                                  exception))))

                    (not (.canWrite file))
                    (throw (ex-info "Durable-atom file is not writeable"
                                    {:file-path file-path}))

                    :else
                    (let [str (slurp file)]
                      (when-not (empty? str)
                        (try (read-string str)
                             (catch RuntimeException exception
                               (throw
                                (ex-info "Could not read durable-atom data"
                                         {:file-path file-path
                                          :content (subs str 0 (min 100 (.length str)))}
                                         exception)))))))))))

(defmethod print-method DurableAtom [x, ^Writer w]
  ((get-method print-method IRecord) x w))

(defn durable-atom
  [file-name]
  (doto (->DurableAtom (atom nil) (agent file-name) (Object.))
    (.read-from-disk)))
