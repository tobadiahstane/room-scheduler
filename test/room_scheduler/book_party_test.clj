(ns room-scheduler.book-party-test
  (:require [clojure.test :refer :all]
            [room-scheduler.core :refer :all]
            [room-scheduler.mock-services :refer :all]
            [room-scheduler.test_functions :refer :all]))


(defn no-booking-confirmed? [result]
  (is (false? (:confirmed result))))

(defn exception-thrown-result-message? [result]
  (is (= "exception thrown") (:message result)))

(defn booking-failed-result-message? [result]
  (is (= "booking failed" (:message result))))

(defn booking-confirmed? [result]
  (is (true? (:confirmed result))))



(defn party-options-not-booked? [check]
  (is (false? (:booked-options @check))))

(defn party-options-booked? [check]
  (is (true? (:booked-options @check))))


(defn party-room-not-reserved? [check]
  (is (false? (:reserved-room @check))))

(defn party-room-reserved? [check]
  (is (true? (:reserved-room @check))))

(defn party-options-not-reserved? [check]
  (is (false? (:reserved-options @check))))

(defn party-options-reserved? [check]
  (is (true? (:reserved-options @check))))


(defn party-room-not-paid? [check]
  (is (false? (:room-fee-paid @check))))

(defn party-room-paid? [check]
  (is (true? (:room-fee-paid @check))))

(defn party-options-paid? [check]
  (is (true? (:options-paid @check))))

(defn party-options-not-paid? [check]
  (is (false? (:options-paid @check))))

(defn party-room-booked? [check]
  (is (true? (:booked-room @check))))

(defn party-room-not-booked? [check]
  (is (false? (:booked-room @check))))



(defn booking-result-confirmation-message? [result]
  (is (= "confirmation msg" (:message result))))


(defn test-book-party [services]
  (book-party {:services services}))

(defn mock-book-pkg-service-given-some-books-party-package [check]
  (->StubBookParty check))

(defn mock-book-room-service-given-some-books-party-room [check]
  (->StubBookParty check))

(defn mock-parse-book-request-given-any-throws-exception []
   (->StubParseBookingException))

(defn mock-book-service-given-any-throws-exception [check]
  (->StubExceptionBookParty check))

(defn mock-book-pkg-service-given-any-fails-silently [check]
  (->StubBookPartyFailsSilently check))

(deftest test-book-party-catches-parse-exceptions
  (let [check (booking-test-check)
        pkg-booker (mock-book-pkg-service-given-some-books-party-package check)
        parser (mock-parse-book-request-given-any-throws-exception)
        booking-services (build-test-booking-services {} check :parser parser)
        result (test-book-party booking-services)]
    (party-room-not-reserved? check)
    (party-options-not-reserved? check)
    (no-booking-confirmed? result)
    (call-exception-handled? check)))

(deftest test-book-party-fails-if-room-timeslots-not-available
  (let [check (booking-test-check)
        confirmer (->StubConfirmPartyTimeSlotNotAvailable check)
        pkg-booker (->StubBookParty check)
        booking-services (build-test-booking-services {} check :confirmer confirmer)
        result (test-book-party booking-services)]
      (party-room-not-reserved? check)
      (party-options-not-reserved? check)
      (no-booking-confirmed? result)
      (no-exception-to-handle? check)
      (booking-failed-result-message? result)))

(deftest test-book-party-fails-if-reserve-room-fails
  (let [check (booking-test-check)
        room-booker (->StubBookPartyRoomReserveThrowsEx check)
        booking-services (build-test-booking-services {} check :room-booker room-booker)
        result (test-book-party booking-services)]
      (party-room-not-reserved? check)
      (party-options-not-reserved? check)
      (no-booking-confirmed? result)
      (call-exception-handled? check)
      (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-reserve-options-fails
  (let [check (booking-test-check)
        options-booker (->StubBookPartyReserveOptionsThrowsEx check)
        booking-services (build-test-booking-services {} check :party-booker options-booker)
        result (test-book-party booking-services)]
      (party-room-reserved? check)
      (party-options-not-reserved? check)
      (no-booking-confirmed? result)
      (call-exception-handled? check)
      (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-process-room-fee-payment-fails
  (let [check (booking-test-check)
        payer (->StubPartyPaymentThrowsEx)
        booking-services (build-test-booking-services {} check :payer payer)
        result (test-book-party booking-services)]
      (party-room-reserved? check)
      (party-options-reserved? check)
      (no-booking-confirmed? result)
      (call-exception-handled? check)
      (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-party-booking-fails
  (let [check (booking-test-check)
        pkg-booker (mock-book-service-given-any-throws-exception check)
        booking-services (build-test-booking-services {} check :room-booker pkg-booker)
        result (test-book-party booking-services)]
    (party-room-not-reserved? check)
    (party-options-not-reserved? check)
    (no-booking-confirmed? result)
    (call-exception-handled? check)
    (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-booking-fails-silently
  (let [check (booking-test-check)
        pkg-booker (mock-book-pkg-service-given-any-fails-silently check)
        booking-services (build-test-booking-services {} check :room-booker pkg-booker :party-booker pkg-booker)
        result (test-book-party booking-services)]
    (no-booking-confirmed? result)
    (no-exception-to-handle? check)
    (booking-failed-result-message? result)))

(deftest test-book-party-process-room-fee-if-room-fee-only-selected
  (let [check (booking-test-check)
        payer (->StubPartyPaymentRoomFeeOnly check)
        booking-services (build-test-booking-services {} check :payer payer)
        result (test-book-party booking-services)]
      (party-room-reserved? check)
      (party-options-reserved? check)
      (party-room-paid? check)
      (party-room-booked? check)
      (party-options-not-paid? check)
      (party-options-not-booked? check)
      (no-exception-to-handle? check)
      (booking-confirmed? result)
      (booking-result-confirmation-message? result)))


(deftest test-book-party-process-room-and-options-if-total-selected
  (let [check (booking-test-check)
        payer (->StubPartyPaymentProcessTotal check)
        booking-services (build-test-booking-services {} check :payer payer)
        result (test-book-party booking-services)]
    (party-room-reserved? check)
    (party-options-reserved? check)
    (party-room-paid? check)
    (party-room-booked? check)
    (party-options-paid? check)
    (booking-confirmed? result)
    (party-options-booked? check)
    (party-room-booked? check)
    (no-exception-to-handle? check)
    (booking-result-confirmation-message? result)))
