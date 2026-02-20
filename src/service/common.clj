(ns service.common)

(defn health-check-http-request-handler
  [_context]
  {:status 200
   :body   {:components {:service :healthy}}})
