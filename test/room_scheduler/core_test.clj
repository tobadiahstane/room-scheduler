(ns room-scheduler.core-test
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

(defrecord StubParseBookingRequest []
  ParseBooking
  (parse-booking-request [stub-parser request]
    (when request
      true)))

(defrecord StubParseBookingException []
  ParseBooking
  (parse-booking-request [nil-parser request] (throw-state-exception nil)))


(defrecord StubExceptionBookParty [check]
    BookParty
    (reserve-party-options [stub-booker party-info]
      (when party-info
        (swap! (:check stub-booker) assoc :reserved-options true)))

    (book-party-options [stub-booker party-info]  (throw-state-exception nil)))

(defrecord StubBookPartyFailsSilently [check]
  BookPartyRoom
  (reserve-party-room [stub-booker party-info] nil)
  (book-party-room [stub-booker party-info] nil)
  BookParty
  (reserve-party-options [stub-booker party-info]
    (when party-info))

  (book-party-options [stub-booker party-info] nil))


(defrecord StubPartyPaymentThrowsEx []
  PartyPayment
  (room-fee-only? [stub-payer party-info] (throw-state-exception nil))
  (process-room-fee [stub-payer party-info] (throw-state-exception nil))
  (process-room-and-options [stub-payer party-info] (throw-state-exception nil)))



(defrecord StubPartyPaymentRoomFeeOnly [check]
  PartyPayment
  (room-fee-only? [stub-payer party-info] true)
  (process-room-fee [stub-payer party-info]
    (when party-info
       (swap! (:check stub-payer) assoc :room-fee-paid true)))
  (process-room-and-options [stub-payer party-info] (throw-state-exception nil)))

(defrecord StubPartyPaymentProcessTotal [check]
  PartyPayment
  (room-fee-only? [stub-payer party-info] false)
  (process-room-fee [stub-payer party-info]  (throw-state-exception nil))
  (process-room-and-options [stub-payer party-info]
    (when party-info
       (do (swap! (:check stub-payer) assoc :room-fee-paid true)
           (swap! (:check stub-payer) assoc :options-paid true)))))

(defrecord StubBookPartyRoomReserveThrowsEx [check]
  BookPartyRoom
  (reserve-party-room [stub-booker party-info]
    (throw-state-exception nil))

  (book-party-room [stub-booker party-info]
    (when party-info
      (swap! (:check stub-booker) assoc :booked-room true))))


(defrecord StubBookPartyReserveOptionsThrowsEx [check]
 BookParty
  (reserve-party-options [stub-booker party-info]  (throw-state-exception nil))

  (book-party-options [stub-booker party-info]
    (when party-info
      (swap! (:check stub-booker) assoc :booked-options true))))



(defrecord StubBookParty [check]
  BookPartyRoom
  (reserve-party-room [stub-booker party-info]
    (when party-info
      (swap! (:check stub-booker) assoc :reserved-room true)))

  (book-party-room [stub-booker party-info]
    (when party-info
      (swap! (:check stub-booker) assoc :booked-room true)))

  BookParty
  (reserve-party-options [stub-booker party-info]
    (when party-info
      (swap! (:check stub-booker) assoc :reserved-options true)))

  (book-party-options [stub-booker party-info]
   (when party-info
     (swap! (:check stub-booker) assoc :booked-options true))))


(defrecord StubConfirmPartyTimeSlotNotAvailable [check]
  ConfirmParty
  (timeslot-available? [confirmer party-info] false)
  (booked? [confirmer party-info]
   (when party-info
     (:booked-room @(:check confirmer))))
  (confirm-booking [confirmer party-info]
   (when party-info
    {:message "confirmation msg"}))
  (confirm-failure [confirmation-service party-info]
   (when party-info
    (ex-info "booking failed" {:bad 1}))))

(defrecord StubConfirmPartyTimeSlotAvailable [check]
  ConfirmParty
  (timeslot-available? [confirmer party-info] true)
  (booked? [confirmer party-info]
   (when party-info
     (:booked-room @(:check confirmer))))
  (confirm-booking [confirmer party-info]
   (when party-info
    {:message "confirmation msg"}))
  (confirm-failure [confirmation-service party-info]
   (when party-info
    (ex-info "booking failed" {:bad 1}))))

(defrecord StubViewBookingResponse []
  RespondWithPartyBooking
  (respond-with-confirmation [stub-viewer result request]
    (when request
      {:confirmed true :message (:message result)}))
  (respond-with-confirmation-failure [stub-viewer result request]
    (when request
      {:confirmed false :message (.getMessage ^Throwable result)})))


(defrecord StubBookingExceptionHandler [check]
  HandleBookPartyException
  (handle-booking-ex [exception-service ex]
    (swap! (:check exception-service) assoc :ex-handled true)))

(defn booking-test-check []
  (atom {:reserved-room false
         :reserved-options false
         :booked-options false
         :booked-room false
         :room-fee-paid false
         :options-paid false
         :ex-handled false}))


(defn build-test-booking-services
  [check & {:keys [parser room-booker party-booker payer confirmer responder ex-handler]
            :or {room-booker (->StubBookParty check)
                 party-booker (->StubBookParty check)
                 confirmer  (->StubConfirmPartyTimeSlotAvailable check)
                 payer (->StubPartyPaymentRoomFeeOnly check)
                 parser (->StubParseBookingRequest)
                 responder (->StubViewBookingResponse)
                 ex-handler  (->StubBookingExceptionHandler check)}}]
  (build-booking-services {}
    :parser parser
    :payer payer
    :confirmer confirmer
    :ex-handler ex-handler
    :responder responder
    :room-booker room-booker
    :party-booker party-booker))


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
        booking-services (build-test-booking-services check :parser parser)
        result (test-book-party booking-services)]
    (party-room-not-reserved? check)
    (party-options-not-reserved? check)
    (no-booking-confirmed? result)
    (call-exception-handled? check)))

(deftest test-book-party-fails-if-room-timeslots-not-available
  (let [check (booking-test-check)
        confirmer (->StubConfirmPartyTimeSlotNotAvailable check)
        pkg-booker (->StubBookParty check)
        booking-services (build-test-booking-services check :confirmer confirmer)
        result (test-book-party booking-services)]
      (party-room-not-reserved? check)
      (party-options-not-reserved? check)
      (no-booking-confirmed? result)
      (no-exception-to-handle? check)
      (booking-failed-result-message? result)))

(deftest test-book-party-fails-if-reserve-room-fails
  (let [check (booking-test-check)
        room-booker (->StubBookPartyRoomReserveThrowsEx check)
        booking-services (build-test-booking-services check :room-booker room-booker)
        result (test-book-party booking-services)]
      (party-room-not-reserved? check)
      (party-options-not-reserved? check)
      (no-booking-confirmed? result)
      (call-exception-handled? check)
      (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-reserve-options-fails
  (let [check (booking-test-check)
        options-booker (->StubBookPartyReserveOptionsThrowsEx check)
        booking-services (build-test-booking-services check :party-booker options-booker)
        result (test-book-party booking-services)]
      (party-room-reserved? check)
      (party-options-not-reserved? check)
      (no-booking-confirmed? result)
      (call-exception-handled? check)
      (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-process-room-fee-payment-fails
  (let [check (booking-test-check)
        payer (->StubPartyPaymentThrowsEx)
        booking-services (build-test-booking-services check :payer payer)
        result (test-book-party booking-services)]
      (party-room-reserved? check)
      (party-options-reserved? check)
      (no-booking-confirmed? result)
      (call-exception-handled? check)
      (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-party-booking-fails
  (let [check (booking-test-check)
        pkg-booker (mock-book-service-given-any-throws-exception check)
        booking-services (build-test-booking-services check :room-booker pkg-booker)
        result (test-book-party booking-services)]
    (party-room-not-reserved? check)
    (party-options-not-reserved? check)
    (no-booking-confirmed? result)
    (call-exception-handled? check)
    (exception-thrown-result-message? result)))

(deftest test-book-party-fails-if-booking-fails-silently
  (let [check (booking-test-check)
        pkg-booker (mock-book-pkg-service-given-any-fails-silently check)
        booking-services (build-test-booking-services check :room-booker pkg-booker :party-booker pkg-booker)
        result (test-book-party booking-services)]
    (no-booking-confirmed? result)
    (no-exception-to-handle? check)
    (booking-failed-result-message? result)))

(deftest test-book-party-process-room-fee-if-room-fee-only-selected
  (let [check (booking-test-check)
        payer (->StubPartyPaymentRoomFeeOnly check)
        booking-services (build-test-booking-services check :payer payer)
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
        booking-services (build-test-booking-services check :payer payer)
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



(deftest test-scheduler-app-get-open-timeslots
  (let [expected-response {:found "times" :not-found "none" :failed "request failed"}
        responder (->StubWebViewOpenSlots expected-response)
        check (lookup-test-check)
        services (build-test-lookup-services check :responder responder)
        test-app (wrap-services scheduler-app services)
        response (test-app {:request-method :get :uri "/times/"})]
    (is (map? response))
    (is (= (:found expected-response) (:body response)))))


(deftest test-scheduler-app-hold-open-timeslots
  (let [check (hold-test-check)
        expected-response {:hold-success "timeslot-held" :hold-failure "no-hold"}
        responder (->StubWebRespondHoldSlots expected-response)
        services (build-test-holding-services check :responder responder)
        test-app (wrap-services scheduler-app services)
        response (test-app {:request-method :post :uri "/times/"})]
    (is (map? response))
    (is (= 200 (:status response)))
    (is (= (:hold-success expected-response) (:body response)))))

(deftest test-scheduler-app-lookup-party-options
  (let [check (options-test-check)
        expected-response {:party-options "options" :options-failure "options fail"}
        responder (->StubWebPartyOptionsResponse expected-response)
        services (build-test-options-services check :responder responder)
        test-app (wrap-services scheduler-app services)
        response (test-app {:request-method :get :uri "/options/"})]
    (is (map? response))
    (is (= 200 (:status response)))
    (is (= (:party-options expected-response) (:body response)))))
