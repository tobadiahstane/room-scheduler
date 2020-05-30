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
    [room-scheduler.book-party :refer [parse-booking
                                       availability-confirmed
                                       book
                                       respond-to-booking
                                       fail-booking
                                       booking-exception]]

    [room-scheduler.web-services :refer [test-web-services]]

    [compojure.core :refer [defroutes
                            GET
                            POST
                            wrap-routes]]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.middleware.params :as params]
    [ring.middleware.keyword-params :as key-params]

    [ring.util.response :as resp])
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

(defn book-party
  [request]
  (let [services (:services request)]
    (try
      (let [party-booking (parse-booking services request)]
        (if (availability-confirmed services party-booking)
          (do (book services party-booking)
              (respond-to-booking services party-booking request))
          (fail-booking services party-booking request)))
      (catch Exception ex
        (booking-exception services ex request)))))


;{:start-time :duration :roomdate-id :room-fee :contact-info :payment-method
; :party-options :party-cost :party-payment-decision )

(defn page [name]
  (str "<html><body>")
  (str "<h1>Select a Party Date</h1>"
        "<form action= \"\\times\\\" method= \"\\get\\\" >"
        "<label for=\"partydate\">Date of Party:</label>"
        "<input type='date' id=partydate name='partydate'>"
        "<input type='submit'>"
        "</form>"
       "</body></html>"))


(defn init-handler [{{name "name"} :params}]
  (resp/response (page name)))

(defn wrap-services [handler services]
  (fn [request]
    (handler (update request :services merge services))))

(defroutes scheduler-routing
  (GET "/" [] init-handler)
  (GET "/book-a-party/" [] init-handler)
  (GET "/times/" [] lookup-open-timeslots)
  (POST "/times/" [] hold-open-timeslots)
  (GET "/options/" [] lookup-party-options)
  (POST "/book/" [] book-party))

(def scheduler-app
  (-> scheduler-routing
    (wrap-services test-web-services)
    (key-params/wrap-keyword-params)
    (params/wrap-params)))


(def dev-handler
  (wrap-reload #'scheduler-app))

(defn run-dev-server
  [port]
  (jetty/run-jetty dev-handler {:port port}))

(defn -main
  [& args]
  (jetty/run-jetty scheduler-app {:port  3000 :join? false}))
