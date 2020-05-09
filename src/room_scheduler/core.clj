(ns room-scheduler.core
  (:require
    [room-scheduler.lookup-open-timeslots :refer [parse-lookup
                                                  lookup-open-slots
                                                  respond-with-results
                                                  lookup-exception]]
    [room-scheduler.hold-open-timeslots :refer [parse-timeslots
                                                hold-timeslots
                                                respond-to-hold
                                                hold-exception]]
    [compojure.core :refer [defroutes
                            GET
                            POST
                            wrap-routes]]
    [ring.adapter.jetty :as jetty])
  (:gen-class))


(defn lookup-open-timeslots
  [lookup-request]
  (let [services (:services lookup-request)]
    (try
      (let [party-info (parse-lookup services lookup-request)
            lookup-results (lookup-open-slots services party-info)]
        (respond-with-results services lookup-results lookup-request))
      (catch Exception failure
        (lookup-exception services failure lookup-request)))))

;the timeslots returned include the cost for that slot

;a room-date is the aggregate and has a specific date, location, name, schedule
;{:id nil
; :date nil
; :changes nil
; :location nil
; :name nil
; :specifics nil
; :schedule nil
; :version nil}
;and onther specifics later defined such as:
;;;Maximum occupancy, cleaning-timing, accessibility features

;(defprotocol ParseHold
;  (parse-hold-request [parser-service request]))
;
;
;(defprotocol HoldOpenTimeslots
; (hold-openings [holding-service hold]))
;
;(defprotocol ConfirmHold
;  (confirm-hold [confirmation-service hold]))
;
;(defprotocol HoldRequestResponse
;  (respond-successful-hold [response-service request])
;  (respond-failed-hold [response-service request]))
;
;(defprotocol HandleHoldPartyException
;  (handle-hold-ex [handling-service ex]))
;

;(defn parse-timeslots
;  "Given map of scheduling application services and a hold request,
;   use the hold parser service to pull details about the timeslots
;   to hold from the request."
;  [services request]
;  (let [parser (:parser-service services)]
;    (parse-hold-request parser request)))
;
;(defn hold-timeslots
;  "Given map of scheduling application services and timeslots to hold,
;   use the hold service to place a hold for requested timeslots."
;  [services timeslots]
;  (let [holder (:holding-service services)]
;     (hold-openings holder timeslots)))
;
;(defn respond-to-hold [services timeslots request]
;  (let [confirmer (:confirmation-service services)
;        responder (:response-service services)]
;    (if (confirm-hold confirmer timeslots)
;      (respond-successful-hold responder request)
;      (respond-failed-hold responder request))))
;
;(defn hold-exception [services ex request]
;  (let [ex-handler (:exception-service services)
;        responder (:response-service services)]
;    (handle-hold-ex ex-handler ex)
;    (respond-failed-hold responder request)))
;
;(defn build-holding-services
;  "Given a map of services and the required holding services,
;   add the hold services."
;  [services & {:keys [parser holder confirmer responder ex-handler]}]
;  (-> services
;    (assoc :parser-service parser)
;    (assoc :holding-service holder)
;    (assoc :confirmation-service confirmer)
;    (assoc :response-service responder)
;    (assoc :exception-service ex-handler)))


(defn hold-open-timeslots
  "Handle a request to set a temporary hold on an open timeslot
   while a user continues on the booking process."
  [request]
  (let [services (:services request)]
    (try
      (let [timeslots (parse-timeslots services request)]
        (hold-timeslots services timeslots)
        (respond-to-hold services timeslots request))
      (catch Exception ex
        (hold-exception services ex request)))))

(defn lookup-party-options [request]
 {})

(defprotocol ParseBooking
  (parse-booking-request [parser-service booking-request]))

(defprotocol BookPartyRoom
 (book-party-room [booking-service party-booking]))

(defprotocol BookParty
 (book-party [booking-service party-booking]))

(defprotocol ConfirmParty
  (booked? [confirmation-service confirmation])
  (confirm-booking [confirmation-service party-booking])
  (confirm-failure [confirmation-service confirmation]))

(defprotocol RespondWithPartyBooking
  (respond-with-confirmation [response-service result request])
  (respond-with-confirmation-failure [response-service failure request]))

(defprotocol HandleBookPartyException
  (handle-booking-ex [exception-service ex]))

;)fn book-room-for-party [booking-services party-booking]
;try

;;1) reserve-room (command) [command-service reserve-room-command] -> RoomDateReserved / exception
  ;;a) load roomdate aggregate -> roomdate aggregate / eventstore exception
  ;;b) apply command to aggregate -> RoomDateReserved / command handler exception
  ;;c) store aggregate to event-store - / eventstore exception

;;2) charge-room-fee (io) [payment-services payment-method room-fee] -> true/false
  ;;a) if charge processed return true

;;3) if charge processed:

;;;;4) book-room (command) [command-service book-room-command])
       ;;;; a) load roomdate aggregate -> roomdate aggregate / eventstore exception
       ;;;; b) apply command to aggregate -> RoomDateFeeProcessed RoomDateBooked / command handler exception
       ;;;; c) store aggregate to event-store - / eventstore exception

;catch command exceptions / payment exceptions /

;)fn book-party [booking-services party-booking]
;try
;;;;5) reserve-party-package (commmand)

;;;;;;6) (optional) charge-party-package-cost [payment-services payment-method party-fee]
;;;;;;7) book-party-package

;catch command exceptions / payment exceptions /

(defn add-party-room-service [services pr-booking]
  (assoc services :party-room-service pr-booking))

(defn add-party-service [services p-booking]
  (assoc services :party-service p-booking))

(defn add-booking-response-service [services br-service]
  (assoc services :response-service br-service))

(defn add-booking-exception-service [services ex-service]
  (assoc services :exception-service ex-service))

(defn add-booking-confirmation-service [services bc-service]
  (assoc services :confirmation-service bc-service))

(defn add-booking-request-parser-service [services bp-service]
  (assoc services :parser-service bp-service))


(defn- parse-booking [services request]
  (let [iparse (:parser-service services)]
    (parse-booking-request iparse request)))


(defn- book [services party-booking]
  (let [ibookrooms (:party-room-service services)
        ibookparties (:party-service services)]
    (book-party-room ibookrooms party-booking)
    (book-party ibookparties party-booking)))

(defn- respond-to-booking [services party-booking request]
  (let [iconfirm (:confirmation-service services)
        irespond  (:response-service services)]
    (if (booked? iconfirm party-booking)
      (respond-with-confirmation irespond
        (confirm-booking iconfirm party-booking) request)
      (respond-with-confirmation-failure irespond
        (confirm-failure iconfirm party-booking) request))))

(defn- booking-exception [services ex request]
  (let [ihandle (:exception-service services)
        irespond (:response-service services)]
    (handle-booking-ex ihandle ex)
    (respond-with-confirmation-failure irespond ex request)))

(defn book-room
  [request]
  (let [services (:services request)]
    (try
      (let [party-booking (parse-booking services request)]
        (book services party-booking)
        (respond-to-booking services party-booking request))
      (catch Exception ex
        (booking-exception services ex request)))))

;{:start-time :duration :roomdate-id :room-fee :contact-info :payment-method
; :party-options :party-cost :party-payment-decision )



(defn wrap-services [handler services]
  (fn [request]
    (handler (assoc request :services services))))

(defroutes scheduler-app
  (GET "/times/" [] lookup-open-timeslots)
  (POST "/times/" [] hold-open-timeslots))

(defn -main
  [& args])
  ;(jetty/run-jetty scheduler-app {:port  3000 :join? false}))
