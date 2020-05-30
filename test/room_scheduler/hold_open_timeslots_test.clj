(ns room-scheduler.hold-open-timeslots-test
  (:require [clojure.test :refer :all]
            [room-scheduler.core :refer :all]
            [room-scheduler.mock-services :refer :all]
            [room-scheduler.test_functions :refer :all]))

(defn open-timeslot-not-held? [result]
  (is (false? (:held-timeslot result))))

(defn open-timeslot-held? [result]
  (is (true? (:held-timeslot result))))



(deftest test-hold-open-timeslots-catches-exceptions
  (let [check (hold-test-check)
        services (build-test-holding-services {} check
                  :parser (mock-hold-request-parser-throws-ex))
        response (hold-open-timeslots {:services services})]
    (open-timeslot-not-held? response)
    (call-exception-handled? check)))

(deftest test-hold-open-timeslots-failed-hold
  (let [check (hold-test-check)
        services (build-test-holding-services {} check
                   :holder (mock-timeslot-holder-given-some-update-check-failure check))
        response (hold-open-timeslots {:services services})]
    (open-timeslot-not-held? response)
    (no-exception-to-handle? check)))

(deftest test-hold-open-timeslots-successful-hold
  (let [check (hold-test-check)
        services (build-test-holding-services {} check)
        response (hold-open-timeslots {:services services})]
    (open-timeslot-held? response)
    (no-exception-to-handle? check)))
