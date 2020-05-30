(ns room-scheduler.lookup-open-timeslots-test
  (:require [clojure.test :refer :all]
            [room-scheduler.core :refer :all]
            [room-scheduler.mock-services :refer :all]
            [room-scheduler.test_functions :refer :all]))

(defn test-lookup-open-timeslot
  [services]
  (lookup-open-timeslots {:services services}))

(defn no-openings-found? [result]
  (is (false? (:openings result))))

(defn openings-found? [result]
  (is (true? (:openings result))))

(defn lookup-call-failed? [result]
  (is (true? (:failed result))))

(defn lookup-call-successful? [result]
  (is (false? (:failed result))))

(deftest test-lookup-open-timeslots-catches-exceptions
  (let [check (lookup-test-check)
        looker (mock-lookup-service-given-any-throws-exception)
        services (build-test-lookup-services {} check :looker looker)
        result (test-lookup-open-timeslot services)]
   (lookup-call-failed? result)
   (call-exception-handled? check)))

(deftest test-lookup-open-timeslots-parser-throws-exception
  (let [check (lookup-test-check)
        parser (mock-parser-given-any-throws-exception)
        services (build-test-lookup-services {} check :parser parser)
        result (test-lookup-open-timeslot services)]
    (lookup-call-failed? result)
    (call-exception-handled? check)))

(deftest test-lookup-open-timeslots-returns-timeslots
  (let [check (lookup-test-check)
        services (build-test-lookup-services {} check)
        result (test-lookup-open-timeslot services)]
    (openings-found? result)
    (lookup-call-successful? result)
    (no-exception-to-handle? check)))

(deftest test-lookup-open-timeslots-returns-no-timeslots
  (let [check (lookup-test-check)
        looker (mock-lookup-service-given-any-returns-nil)
        services (build-test-lookup-services {} check :looker looker)
        result (test-lookup-open-timeslot services)]
    (no-openings-found? result)
    (lookup-call-successful? result)
    (no-exception-to-handle? check)))
