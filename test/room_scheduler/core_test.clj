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



;(defrecord StubParseHoldRequest []
;  ParseHold
;  (parse-hold-request [stub-parser request]
;    (when request
;      true)))
;
;(defrecord StubHoldRequestParserThrowsEx []
;  ParseBooking
;  (parse-booking-request [stub-parser request] (throw-state-exception nil)))
;
;(defrecord StubHoldExceptionHandler [check]
;  HandleHoldPartyException
;  (handle-hold-ex [stub-handler ex]
;    (swap! (:check stub-handler) assoc :ex-handled true)))
;
;(defrecord StubHoldTimeslots [check]
;  HoldOpenTimeslots
;  (hold-openings [stub-holder request]
;    (when request
;      (swap! (:check stub-holder) assoc :openings-held true))))
;
;(defrecord StubHoldTimeslotsFails [check]
;  HoldOpenTimeslots
;  (hold-openings [stub-holder request] nil))
;
;(defrecord StubHoldConfirmer [check]
;  ConfirmHold
;  (confirm-hold [stub-confirmer hold]
;    (:openings-held @(:check stub-confirmer))))
;
;(defrecord StubHoldResponse []
;  HoldRequestResponse
;  (respond-successful-hold [stub-responder request]
;    {:held-timeslot true})
;  (respond-failed-hold [stub-responder request]
;    {:held-timeslot false}))


;(defrecord StubWebRespondHoldSlots [responses]
;  HoldRequestResponse
;  (respond-successful-hold [stub-responder request]
;    {:body (:hold-success (:responses stub-responder))})
;  (respond-failed-hold [stub-responder request]
;    {:body (:hold-failure (:responses stub-responder))}))

;(defn hold-test-check
;  []
;  (atom {:ex-handled false
;         :openings-held false}))
;
;(defn open-timeslot-not-held? [result]
;  (is (false? (:held-timeslot result))))
;
;(defn open-timeslot-held? [result]
;  (is (true? (:held-timeslot result))))
;
;(defn hold-request-parser-throws-ex []
;  (->StubHoldRequestParserThrowsEx))
;
;(defn hold-timeslot-service-fails [check]
;  (->StubHoldTimeslotsFails check))
;
;(defn build-test-holding-services
;  [check & {:keys [parser holder confirmer responder ex-handler]
;            :or {parser (->StubParseHoldRequest)
;                 holder (->StubHoldTimeslots check)
;                 confirmer (->StubHoldConfirmer check)
;                 responder (->StubHoldResponse)
;                 ex-handler (->StubHoldExceptionHandler check)}}]
;  (build-holding-services {}
;    :parser parser
;    :holder holder
;    :confirmer confirmer
;    :responder responder
;    :ex-handler ex-handler))
;
;(deftest test-hold-open-timeslots-catches-exceptions
;  (let [check (hold-test-check)
;        services (build-test-holding-services check
;                  :parser (hold-request-parser-throws-ex))
;        response (hold-open-timeslots {:services services})]
;    (open-timeslot-not-held? response)
;    (call-exception-handled? check)))
;
;(deftest test-hold-open-timeslots-failed-hold
;  (let [check (hold-test-check)
;        services (build-test-holding-services check
;                   :holder (hold-timeslot-service-fails check))
;        response (hold-open-timeslots {:services services})]
;    (open-timeslot-not-held? response)
;    (no-exception-to-handle? check)))
;
;(deftest test-hold-open-timeslots-successful-hold
;  (let [check (hold-test-check)
;        services (build-test-holding-services check)
;        response (hold-open-timeslots {:services services})]
;    (open-timeslot-held? response)
;    (no-exception-to-handle? check)))
;

(deftest test-lookup-party-options
  (is (some? (lookup-party-options nil))))


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
    (book-party [stub-booker party-info]  (throw-state-exception nil)))

(defrecord StubBookPartyFailsSilently [check]
  BookPartyRoom
  (book-party-room [stub-booker party-info] nil)
  BookParty
  (book-party [stub-booker party-info] nil))

(defrecord StubBookParty [check]
  BookPartyRoom
  (book-party-room [stub-booker party-info]
    (when party-info
      (swap! (:check stub-booker) assoc :booked-room true)))

  BookParty
  (book-party [stub-booker party-info]
   (when party-info
     (swap! (:check stub-booker) assoc :booked-party true))))


(defrecord StubConfirmParty [check]
  ConfirmParty
  (booked? [confirmer party-info]
   (when party-info
     (:booked-party @(:check confirmer))))
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
  (atom {:booked-party false
         :booked-room false
         :ex-handled false}))

(defn build-test-booking-services
  ([check party-booker]
   (build-test-booking-services check party-booker (->StubParseBookingRequest)))
  ([check party-booker parser]
   (-> {}
     (add-booking-confirmation-service (->StubConfirmParty check))
     (add-booking-request-parser-service parser)
     (add-booking-exception-service (->StubBookingExceptionHandler check))
     (add-booking-response-service (->StubViewBookingResponse))
     (add-party-room-service (->StubBookParty check))
     (add-party-service party-booker))))

(defn no-booking-confirmed? [result]
  (is (false? (:confirmed result))))

(defn exception-thrown-result-message? [result]
  (is (= "exception thrown") (:message result)))

(defn booking-failed-result-message? [result]
  (is (= "booking failed" (:message result))))

(defn booking-confirmed? [result]
  (is (true? (:confirmed result))))

(defn party-package-booked? [check]
  (is (true? (:booked-party @check))))

(defn party-room-booked? [check]
  (is (true? (:booked-room @check))))

(defn booking-result-confirmation-message? [result]
  (is (= "confirmation msg" (:message result))))


(defn test-book-party [services]
  (book-room {:services services}))

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

(deftest test-book-room-catches-parse-exceptions
  (let [check (booking-test-check)
        pkg-booker (mock-book-pkg-service-given-some-books-party-package check)
        parser (mock-parse-book-request-given-any-throws-exception)
        booking-services (build-test-booking-services check pkg-booker parser)
        result (test-book-party booking-services)]
    (no-booking-confirmed? result)
    (call-exception-handled? check)))

(deftest test-book-room-catches-party-booking-exceptions
  (let [check (booking-test-check)
        pkg-booker (mock-book-service-given-any-throws-exception check)
        booking-services (build-test-booking-services check pkg-booker)
        result (test-book-party booking-services)]
    (no-booking-confirmed? result)
    (call-exception-handled? check)
    (exception-thrown-result-message? result)))

(deftest test-book-room-book-party-package-fails-silently
  (let [check (booking-test-check)
        pkg-booker (mock-book-pkg-service-given-any-fails-silently check)
        booking-services (build-test-booking-services check pkg-booker)
        result (test-book-party booking-services)]
    (no-booking-confirmed? result)
    (no-exception-to-handle? check)
    (booking-failed-result-message? result)))

(deftest test-book-room-book-party-confirms-sucessful-booking
  (let [check (booking-test-check)
        pkg-booker (->StubBookParty check)
        booking-services (build-test-booking-services check pkg-booker)
        result (test-book-party booking-services)]
    (booking-confirmed? result)
    (party-package-booked? check)
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
