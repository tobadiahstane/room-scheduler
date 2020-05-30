(ns room-scheduler.lookup-party-options)


(defprotocol ParsePartyOptionsRequest
  (parse-party-options-request [parser-service request]))

(defprotocol LookUpPartyOptions
  (get-options [lookup-service lookup]))

(defprotocol RespondWithPartyOptions
  (options-response [response-service options request])
  (respond-failed [response-service ex request]))

(defprotocol HandleLookupPartyOptionsException
  (handle-options-ex [handling-service ex]))

(defn parse-options-request [services request]
   (let [parser (::parser-service services)]
      (parse-party-options-request parser request)))


(defn lookup-options [services lookup]
  (let [looker (::lookup-service services)]
     (get-options looker lookup)))

(defn respond-with-options [services options request]
   (let [responder (::response-service services)]
     (options-response responder options request)))

(defn options-lookup-exception [services ex request]
  (let [ex-handler (::exception-service services)
        responder (::response-service services)]
    (handle-options-ex ex-handler ex)
    (respond-failed responder ex request)))


(defn add-party-options-lookup-service [services service]
  (assoc services ::lookup-service service))

(defn add-party-options-request-parser-service [services service]
  (assoc services ::parser-service service))

(defn add-party-options-response-service [services service]
  (assoc services ::response-service service))

(defn add-party-options-exception-service [services service]
  (assoc services ::exception-service service))

(defn build-party-options-lookup-services
   [services & {:keys [parser looker responder ex-handler]}]
   (-> services
     (add-party-options-request-parser-service parser)
     (add-party-options-lookup-service looker)
     (add-party-options-response-service responder)
     (add-party-options-exception-service ex-handler)))
