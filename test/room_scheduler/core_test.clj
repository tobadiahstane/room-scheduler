(ns room-scheduler.core-test
  (:require [clojure.test :refer :all]
            [room-scheduler.core :refer :all]
            [room-scheduler.mock-services :refer :all]
            [room-scheduler.test_functions :refer :all]))




(deftest test-scheduler-app-test-init-page
  (let [test-app scheduler-app
        response (test-app {:request-method :get :uri "/book-a-party/"})]
    (is (map? response))))

(deftest test-scheduler-app-test-specific-date
  (let [test-app scheduler-app
        response (test-app {:request-method :get :uri "/times/" :query-string "partydate=2020-07-17"})]
    (is (map? response))
    (is (= 200 (:status response)))))


(deftest test-scheduler-app-hold-open-timeslots
  (let [check (hold-test-check)
        expected-response {:hold-success "timeslot-held" :hold-failure "no-hold"}
        responder (->StubWebRespondHoldSlots expected-response)
        services (build-test-holding-services {} check :responder responder)
        test-app (wrap-services scheduler-app services)
        response (test-app {:request-method :post :uri "/times/"})]
    (is (map? response))
    (is (= 200 (:status response)))
    (is (= (:hold-success expected-response) (:body response)))))

(deftest test-scheduler-app-lookup-party-options
  (let [check (options-test-check)
        expected-response {:party-options "options" :options-failure "options fail"}
        responder (->StubWebPartyOptionsResponse expected-response)
        services (build-test-options-services {} check :responder responder)
        test-app (wrap-services scheduler-app services)
        response (test-app {:request-method :get :uri "/options/"})]
    (is (map? response))
    (is (= 200 (:status response)))
    (is (= (:party-options expected-response) (:body response)))))

(deftest test-scheduler-app-book-party
  (let [check (booking-test-check)
         payer (->StubPartyPaymentRoomFeeOnly check)
        expected-response {:confirmed "success" :failure "failed"}
        responder (->StubWebViewBookingResponse expected-response)
        services (build-test-booking-services {} check
                   :payer payer
                   :responder responder)
        test-app (wrap-services scheduler-app services)
        response (test-app {:request-method :post :uri "/book/"})]
    (is (map? response))
    (is (= 200 (:status response)))
    (is (= (:party-options expected-response) (:body response)))))


;test hold response redirects to get lookup-options.
