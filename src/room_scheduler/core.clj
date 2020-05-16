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
    [room-scheduler.lookup-party-options :refer [parse-options-request
                                                 lookup-options
                                                 respond-with-options
                                                 options-lookup-exception]]
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
 (let [services (:services request)]
  (try
    (let [lookup (parse-options-request services request)
          options (lookup-options services lookup)]
      (respond-with-options services options request))
    (catch Exception ex
      (options-lookup-exception services ex request)))))

(defprotocol ParseBooking
  (parse-booking-request [parser-service booking-request]))

(defprotocol BookPartyRoom
  (reserve-party-room [booking-service party-booking])
  (book-party-room [booking-service party-booking]))

(defprotocol PartyPayment
  (room-fee-only? [payment-service party-booking])
  (process-room-fee [payment-service party-booking])
  (process-room-and-options [payment-service party-booking]))

(defprotocol BookParty
  (reserve-party-options [booking-service party-booking])
  (book-party-options [booking-service party-booking]))

(defprotocol ConfirmParty
   (timeslot-available? [confirmation-service party-booking])
   (booked? [confirmation-service confirmation])
   (confirm-booking [confirmation-service party-booking])
   (confirm-failure [confirmation-service confirmation]))

(defprotocol RespondWithPartyBooking
   (respond-with-confirmation [response-service result request])
   (respond-with-confirmation-failure [response-service failure request]))

(defprotocol HandleBookPartyException
   (handle-booking-ex [exception-service ex]))

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

(defn add-party-payment-processor-service [services service]
  (assoc services :payment-service service))


(defn- parse-booking [services request]
  (let [iparse (:parser-service services)]
    (parse-booking-request iparse request)))


(defn- book [services party-booking]
  (let [ibookrooms (:party-room-service services)
        ibookparties (:party-service services)
        ipay (:payment-service services)]
    (reserve-party-room ibookrooms party-booking)
    (reserve-party-options ibookparties party-booking)
    (if (room-fee-only? ipay party-booking)
      (do (process-room-fee ipay party-booking)
        (book-party-room ibookrooms party-booking))
      (do (process-room-and-options ipay party-booking)
        (book-party-room ibookrooms party-booking)
        (book-party-options ibookparties party-booking)))))

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

(defn book-party
  [request]
  (let [services (:services request)]
    (try
      (let [party-booking (parse-booking services request)
            iconfirm (:confirmation-service services)
            irespond  (:response-service services)]
        (if (timeslot-available? iconfirm party-booking)
          (do (book services party-booking)
              (respond-to-booking services party-booking request))
          (respond-with-confirmation-failure irespond
             (confirm-failure iconfirm party-booking) request)))
      (catch Exception ex
        (booking-exception services ex request)))))

(defn build-booking-services
  [services & {:keys [room-booker party-booker payer confirmer parser responder ex-handler]}]
  (-> services
    (add-booking-confirmation-service confirmer)
    (add-booking-request-parser-service parser)
    (add-party-payment-processor-service payer)
    (add-booking-exception-service ex-handler)
    (add-booking-response-service responder)
    (add-party-room-service room-booker)
    (add-party-service party-booker)))

;{:start-time :duration :roomdate-id :room-fee :contact-info :payment-method
; :party-options :party-cost :party-payment-decision )



(defn wrap-services [handler services]
  (fn [request]
    (handler (assoc request :services services))))

(defroutes scheduler-app
  (GET "/times/" [] lookup-open-timeslots)
  (POST "/times/" [] hold-open-timeslots)
  (GET "/options/" [] lookup-party-options))

(defn -main
  [& args])
  ;(jetty/run-jetty scheduler-app {:port  3000 :join? false}))
