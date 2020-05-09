(ns room-scheduler.lookup-open-timeslots)

(defprotocol LookupOpenTimeSlots
  (get-openings [lookup-service lookup-request]))

(defprotocol ParseLookupRequest
  (parse-lookup-request [lookup-service lookup-request]))

(defprotocol RespondWithOpenTimeSlots
  (respond-with-openings [response-service open-slots lookup-request])
  (respond-with-no-openings [response-service lookup-request])
  (respond-with-lookup-failure [response-service failure request]))

(defprotocol HandleLookupOpenSlotsException
  (handle-lookup-ex [exception-service ex]))

(defn add-open-timeslot-lookup-service [services otl-service]
  (assoc services :lookup-service otl-service))

(defn add-open-timeslot-request-parser-service [services bp-service]
  (assoc services :parser-service bp-service))

(defn add-open-timeslot-response-service [services otr-service]
  (assoc services :response-service otr-service))

(defn add-open-timeslot-exception-service [services ex-service]
  (assoc services :exception-service ex-service))


(defn parse-lookup [services lookup-request]
  (let [iparse (:parser-service services)]
    (parse-lookup-request iparse lookup-request)))

(defn lookup-open-slots [services party-info]
  (let [ilookup (:lookup-service services)]
    (get-openings ilookup party-info)))

(defn respond-with-results [services lookup-results lookup-request]
  (let [irespond (:response-service services)]
    (if lookup-results
       (respond-with-openings irespond lookup-results lookup-request)
       (respond-with-no-openings irespond lookup-request))))

(defn lookup-exception [services failure lookup-request]
  (let [ihandle (:exception-service services)
        irespond (:response-service services)]
    (handle-lookup-ex ihandle failure)
    (respond-with-lookup-failure irespond failure lookup-request)))

(defn build-lookup-services
   [services & {:keys [parser looker responder ex-handler]}]
   (-> services
     (add-open-timeslot-request-parser-service parser)
     (add-open-timeslot-lookup-service looker)
     (add-open-timeslot-response-service responder)
     (add-open-timeslot-exception-service ex-handler)))