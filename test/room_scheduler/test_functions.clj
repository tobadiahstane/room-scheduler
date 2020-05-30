(ns room-scheduler.test_functions
  (:require [clojure.test :refer :all]))

(defn call-exception-handled?
  [check]
  (is (true? (:ex-handled @check))))

(defn no-exception-to-handle?
  [check]
  (is (false? (:ex-handled @check))))

(defn make-check [check-map]
  (atom check-map))
