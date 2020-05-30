(ns room-scheduler.mock-services
  (:require
   [room-scheduler.test_functions :refer [make-check]]

   [room-scheduler.lookup-open-timeslots :as lookup-times
                                         :refer [LookupOpenTimeSlots
                                                 ParseLookupRequest
                                                 RespondWithOpenTimeSlots
                                                 HandleLookupOpenSlotsException]]
   [room-scheduler.hold-open-timeslots :as hold
                                       :refer [ParseHold
                                               HoldOpenTimeslots
                                               ConfirmHold
                                               HoldRequestResponse
                                               HandleHoldPartyException
                                               build-holding-services]]

   [room-scheduler.lookup-party-options :as lookup-options
                                        :refer [ParsePartyOptionsRequest
                                                LookUpPartyOptions
                                                RespondWithPartyOptions
                                                HandleLookupPartyOptionsException]]
   [room-scheduler.book-party :as book
                              :refer [ParseBooking
                                      BookPartyRoom
                                      PartyPayment
                                      BookParty
                                      ConfirmParty
                                      RespondWithPartyBooking
                                      HandleBookPartyException]]))
(defn throw-state-exception [request]
  (throw (IllegalStateException. "exception thrown")))

(defrecord StubExceptionOpenSlots []
  LookupOpenTimeSlots
  (get-openings [throw-service party-info]
    (throw-state-exception party-info)))

(defrecord StubLookupOpenSlots []
  LookupOpenTimeSlots
  (get-openings [lookup-service party-info]
   (if (true? party-info)
    true)))

(defrecord StubLookupNoOpenSlots []
  LookupOpenTimeSlots
  (get-openings [lookup-service party-info] nil))

(defrecord StubParseLookupRequest []
  ParseLookupRequest
  (parse-lookup-request [stub-parser request]
    (when request
      true)))

(defrecord StubLookupExceptionParseRequest []
  ParseLookupRequest
  (parse-lookup-request [nil-parser request] (throw-state-exception nil)))



(defrecord StubViewOpenSlots []
  RespondWithOpenTimeSlots
  (respond-with-openings [stub-viewer open-slots request]
    (when request
      {:openings true :failed false}))
  (respond-with-no-openings [stub-viewer request]
    (when request
      {:openings false :failed false}))
  (respond-with-lookup-failure [response-service failure request]
    (when request
      {:openings nil :failed true})))


(defrecord StubViewOpenSlots []
  RespondWithOpenTimeSlots
  (respond-with-openings [stub-viewer open-slots request]
    (when request
      {:openings true :failed false}))
  (respond-with-no-openings [stub-viewer request]
    (when request
      {:openings false :failed false}))
  (respond-with-lookup-failure [response-service failure request]
    (when request
      {:openings nil :failed true})))


(defrecord StubWebViewOpenSlots [responses]
  RespondWithOpenTimeSlots
  (respond-with-openings [stub-viewer open-slots request]
    (when request
      {:body (:found (:responses stub-viewer))}))
  (respond-with-no-openings [stub-viewer request]
    (when request
      {:body (:not-found (:responses stub-viewer))}))
  (respond-with-lookup-failure [response-service failure request]
    (when request
      {:body (:failed (:responses response-service))})))

(defrecord StubLookupExceptionHandler [check]
  HandleLookupOpenSlotsException
  (handle-lookup-ex [exception-service ex]
    (swap! (:check exception-service) assoc :ex-handled true)))


(defn mock-lookup-service-given-true-returns-true []
 (->StubLookupOpenSlots))

(defn mock-lookup-service-given-any-throws-exception []
  (->StubExceptionOpenSlots))

(defn mock-lookup-service-given-any-returns-nil []
  (->StubLookupNoOpenSlots))

(defn mock-parser-given-any-throws-exception []
  (->StubLookupExceptionParseRequest))

(defn mock-parser-given-some-returns-true []
  (->StubParseLookupRequest))

(defn mock-responder-given-true-return-true []
  (->StubViewOpenSlots))

(defn mock-ex-handler-updates-check-when-handled [check]
  (->StubLookupExceptionHandler check))

(defn mock-responder-gives-expected-response [expected-responses]
   (map->StubWebViewOpenSlots expected-responses))

(defn lookup-test-check []
  (make-check {:ex-handled false}))

(defn build-test-lookup-services
  [services check & {:keys [looker parser responder ex-handler]
                      :or {looker (mock-lookup-service-given-true-returns-true)
                           parser (mock-parser-given-some-returns-true)
                           responder (mock-responder-given-true-return-true)
                           ex-handler (mock-ex-handler-updates-check-when-handled check)}}]
  (lookup-times/build-lookup-services services
    :parser parser
    :looker looker
    :responder responder
    :ex-handler ex-handler))

(defrecord StubParseHoldRequest []
  ParseHold
  (parse-hold-request [stub-parser request]
    (when request
      true)))

(defrecord StubHoldRequestParserThrowsEx []
  ParseHold
  (parse-hold-request [stub-parser request] (throw-state-exception nil)))

(defrecord StubHoldExceptionHandler [check]
  HandleHoldPartyException
  (handle-hold-ex [stub-handler ex]
    (swap! (:check stub-handler) assoc :ex-handled true)))

(defrecord StubHoldTimeslots [check]
  HoldOpenTimeslots
  (hold-openings [stub-holder request]
    (when request
      (swap! (:check stub-holder) assoc :openings-held true))))

(defrecord StubHoldTimeslotsFails [check]
  HoldOpenTimeslots
  (hold-openings [stub-holder request] nil))

(defrecord StubHoldConfirmer [check]
  ConfirmHold
  (confirm-hold [stub-confirmer hold]
    (:openings-held @(:check stub-confirmer))))

(defrecord StubHoldResponse []
  HoldRequestResponse
  (respond-successful-hold [stub-responder request]
    {:held-timeslot true})
  (respond-failed-hold [stub-responder request]
    {:held-timeslot false}))

(defrecord StubWebRespondHoldSlots [responses]
  HoldRequestResponse
  (respond-successful-hold [stub-responder request]
    {:body (:hold-success (:responses stub-responder))})
  (respond-failed-hold [stub-responder request]
    {:body (:hold-failure (:responses stub-responder))}))

(defn hold-test-check
  []
  (make-check {:ex-handled false
               :openings-held false}))

(defn mock-hold-request-parser-given-some-returns-true []
  (->StubParseHoldRequest))

(defn mock-hold-request-parser-throws-ex []
  (->StubHoldRequestParserThrowsEx))

(defn mock-timeslot-holder-given-some-update-check-success [check]
  (->StubHoldTimeslots check))

(defn mock-timeslot-holder-given-some-update-check-failure [check]
  (->StubHoldTimeslotsFails check))


(defn mock-hold-confirmer-given-check-returns-holder-update [check]
  (->StubHoldConfirmer check))

(defn mock-responder-returns-held-timeslot-success-fail []
  (->StubHoldResponse))

(defn mock-hold-ex-handler-updates-check [check]
  (->StubHoldExceptionHandler check))

(defn build-test-holding-services
  [services check & {:keys [parser holder confirmer responder ex-handler]
                     :or {parser (mock-hold-request-parser-given-some-returns-true)
                          holder (mock-timeslot-holder-given-some-update-check-success check)
                          confirmer (mock-hold-confirmer-given-check-returns-holder-update check)
                          responder (mock-responder-returns-held-timeslot-success-fail)
                          ex-handler (mock-hold-ex-handler-updates-check check)}}]
  (build-holding-services services
    :parser parser
    :holder holder
    :confirmer confirmer
    :responder responder
    :ex-handler ex-handler))

(defn options-test-check
  []
  (make-check {:ex-handled false}))

(defrecord StubParsePartyOptionsRequest []
  ParsePartyOptionsRequest
  (parse-party-options-request [stub-parser request]
    (when request
      true)))

(defrecord StubParsePartyOptionsRequestThrowsEx []
  ParsePartyOptionsRequest
  (parse-party-options-request [stub-parser request] (throw-state-exception nil)))

(defrecord StubPartyOptionsExceptionHandler [check]
  HandleLookupPartyOptionsException
  (handle-options-ex [stub-handler ex]
    (swap! (:check stub-handler) assoc :ex-handled true)))

(defrecord StubLookUpPartyOptions []
  LookUpPartyOptions
  (get-options [stub-lookup request]
    (when request
     true)))

(defrecord StubLookUpPartyOptionsException []
  LookUpPartyOptions
  (get-options [stub-holder request]
    (throw-state-exception nil)))

(defrecord StubPartyOptionsResponse []
  RespondWithPartyOptions
  (options-response [stub-responder options request]
    {:party-options true})
  (respond-failed [stub-responder ex request]
    {:party-options false}))

(defrecord StubWebPartyOptionsResponse [responses]
  RespondWithPartyOptions
  (options-response [stub-responder options request]
    {:body (:party-options (:responses stub-responder))})
  (respond-failed [stub-responder ex request]
    {:body (:options-failure (:responses stub-responder))}))


(defn options-request-parser-throws-ex []
  (->StubParsePartyOptionsRequestThrowsEx))

(defn lookup-party-options-service-fails []
  (->StubLookUpPartyOptionsException))

(defn build-test-options-services
  [services check & {:keys [parser looker responder ex-handler]
                     :or {parser (->StubParsePartyOptionsRequest)
                          looker (->StubLookUpPartyOptions)
                          responder (->StubPartyOptionsResponse)
                          ex-handler (->StubPartyOptionsExceptionHandler check)}}]
  (lookup-options/build-party-options-lookup-services services
    :parser parser
    :looker looker
    :responder responder
    :ex-handler ex-handler))
;

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


(defrecord StubWebViewBookingResponse [expected-response]
  RespondWithPartyBooking
  (respond-with-confirmation [stub-responder result request]
    (when request
      {:body (:confirmed (:responses stub-responder))}))
  (respond-with-confirmation-failure [stub-responder result request]
    {:body (:failure (:responses stub-responder))}))


(defn booking-test-check []
  (make-check {:reserved-room false
               :reserved-options false
               :booked-options false
               :booked-room false
               :room-fee-paid false
               :options-paid false
               :ex-handled false}))


(defn build-test-booking-services
  [services check & {:keys [parser room-booker party-booker payer confirmer responder ex-handler]
                     :or {room-booker (->StubBookParty check)
                          party-booker (->StubBookParty check)
                          confirmer  (->StubConfirmPartyTimeSlotAvailable check)
                          payer (->StubPartyPaymentRoomFeeOnly check)
                          parser (->StubParseBookingRequest)
                          responder (->StubViewBookingResponse)
                          ex-handler  (->StubBookingExceptionHandler check)}}]
  (book/build-booking-services services
    :parser parser
    :payer payer
    :confirmer confirmer
    :ex-handler ex-handler
    :responder responder
    :room-booker room-booker
    :party-booker party-booker))
