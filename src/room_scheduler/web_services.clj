(ns room-scheduler.web-services
  (:require
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
                                      HandleBookPartyException]]
   [ring.util.response :as resp]))



(def party-options )



(defrecord ExceptionHandler []
  HandleLookupOpenSlotsException
  (handle-lookup-ex [exception-service ex]
   (println "Lookup open timeslot exception handled")
   (println "")
   (println (.getMessage ^Throwable ex))
   (println "")
   (println "______")))


(defrecord WebParseLookupRequest []
  ParseLookupRequest
  (parse-lookup-request [parser request]
    (let [params (:params request)]
     (println "Lookup open timeslots for params")
     (println "")
     (println params)
     (println "")
     (println "______")
     params)))

(defrecord WebLookupOpenSlots []
  LookupOpenTimeSlots
  (get-openings [lookup-service party-info]
   (if (:partydate party-info)
    "true"
    (throw (IllegalStateException. "No partydate Exception thrown")))))

(defrecord WebViewOpenSlots []
  RespondWithOpenTimeSlots
  (respond-with-openings [stub-viewer open-slots request]
    (println "openings found")
    (resp/response open-slots))
  (respond-with-no-openings [stub-viewer request]
    (println "no openings found"
     (resp/response "No openings found")))
  (respond-with-lookup-failure [response-service failure request]
      (resp/response (:message failure))))


(defrecord ViewBookingResponse [expected-response]
  RespondWithPartyBooking
  (respond-with-confirmation [stub-responder result request]
      {:body (:confirmed (:responses stub-responder))})
  (respond-with-confirmation-failure [stub-responder result request]
    {:body (:failure (:responses stub-responder))}))

(def test-web-services
  (let [exception-handler (->ExceptionHandler)]
    (lookup-times/build-lookup-services {}
      :parser (->WebParseLookupRequest)
      :looker (->WebLookupOpenSlots)
      :responder (->WebViewOpenSlots)
      :ex-handler exception-handler)))
