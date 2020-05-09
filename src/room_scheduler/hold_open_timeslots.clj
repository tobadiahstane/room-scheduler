(ns room-scheduler.hold-open-timeslots)



(defprotocol ParseHold
  (parse-hold-request [parser-service request]))

(defprotocol HoldOpenTimeslots
 (hold-openings [holding-service hold]))

(defprotocol ConfirmHold
  (confirm-hold [confirmation-service hold]))

(defprotocol HoldRequestResponse
  (respond-successful-hold [response-service request])
  (respond-failed-hold [response-service request]))

(defprotocol HandleHoldPartyException
  (handle-hold-ex [handling-service ex]))


(defn parse-timeslots
  "Given map of scheduling application services and a hold request,
   use the hold parser service to pull details about the timeslots
   to hold from the request."
  [services request]
  (let [parser (:parser-service services)]
    (parse-hold-request parser request)))

(defn hold-timeslots
  "Given map of scheduling application services and timeslots to hold,
   use the hold service to place a hold for requested timeslots."
  [services timeslots]
  (let [holder (:holding-service services)]
     (hold-openings holder timeslots)))

(defn respond-to-hold [services timeslots request]
  (let [confirmer (:confirmation-service services)
        responder (:response-service services)]
    (if (confirm-hold confirmer timeslots)
      (respond-successful-hold responder request)
      (respond-failed-hold responder request))))

(defn hold-exception [services ex request]
  (let [ex-handler (:exception-service services)
        responder (:response-service services)]
    (handle-hold-ex ex-handler ex)
    (respond-failed-hold responder request)))

(defn build-holding-services
  "Given a map of services and the required holding services,
   add the hold services."
  [services & {:keys [parser holder confirmer responder ex-handler]}]
  (-> services
    (assoc :parser-service parser)
    (assoc :holding-service holder)
    (assoc :confirmation-service confirmer)
    (assoc :response-service responder)
    (assoc :exception-service ex-handler)))
