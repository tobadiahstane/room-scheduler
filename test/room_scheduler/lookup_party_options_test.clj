(ns room-scheduler.lookup-party-options-test
  (:require [clojure.test :refer :all]
            [room-scheduler.core :refer :all]
            [room-scheduler.mock-services :refer :all]))

(defn call-exception-handled?
  [check]
  (is (true? (:ex-handled @check))))

(defn no-exception-to-handle?
  [check]
  (is (false? (:ex-handled @check))))

(defn options-test-check
  []
  (atom {:ex-handled false}))

(defn party-options-found? [result]
  (is (true? (:party-options result))))

(defn party-options-not-found? [result]
  (is (false? (:party-options result))))

(deftest test-lookup-party-options-catches-exceptions
  (let [check (options-test-check)
        services (build-test-options-services check
                  :parser (options-request-parser-throws-ex))
        response (lookup-party-options {:services services})]
    (party-options-not-found? response)
    (call-exception-handled? check)))

(deftest test-lookup-party-options-failed
  (let [check (options-test-check)
        services (build-test-options-services check
                  :looker lookup-party-options-service-fails)
        response (lookup-party-options {:services services})]
    (party-options-not-found? response)
    (call-exception-handled? check)))

(deftest test-lookup-party-options-success
  (let [check (options-test-check)
        services (build-test-options-services check)
        response (lookup-party-options {:services services})]
    (party-options-found? response)
    (no-exception-to-handle? check)))
