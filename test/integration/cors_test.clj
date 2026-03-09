(ns cors-test
  (:require [clj-http.client :as http]
            [clojure.test :refer [is testing]]
            [common-clj.integrant-components.config :as component.config]
            [common-clj.integrant-components.prometheus :as component.prometheus]
            [common-clj.integrant-components.routes :as component.routes]
            [integrant.core :as ig]
            [io.pedestal.service.interceptors :as pedestal.service.interceptors]
            [matcher-combinators.test :refer [match?]]
            [schema.test :as s]
            [service.component :as component.service]
            [service.interceptors :as interceptors]))

(def routes [["/test" :get [interceptors/http-request-in-handle-timing-interceptor
                            pedestal.service.interceptors/json-body
                            (fn [{{:keys [config]} :components}]
                              {:status 200
                               :body   config})]
              :route-name :test]])

(def system-components
  {::component.config/config         {:path "test/resources/config.example.edn"
                                      :env  :test}
   ::component.routes/routes         {:routes routes}
   ::component.prometheus/prometheus {:metrics []}
   ::component.service/service       {:components {:config     (ig/ref ::component.config/config)
                                                   :prometheus (ig/ref ::component.prometheus/prometheus)
                                                   :routes     (ig/ref ::component.routes/routes)}}})

(s/deftest allowed-origins-allow-all-test
  (testing "When no allowed-origins is configured, all origins are permitted"
    (let [system (ig/init system-components)]

      (is (match? {:status  200
                   :headers {"Access-Control-Allow-Origin" "https://any-origin.com"}}
                  (http/get "http://localhost:8080/test"
                            {:headers          {"Origin"        "https://any-origin.com"
                                                "authorization" "Bearer test-token"}
                             :throw-exceptions false})))

      (ig/halt! system))))

(s/deftest allowed-origins-restricted-test
  (testing "When allowed-origins is configured, only specified origins are permitted"
    (let [system (ig/init (assoc-in system-components
                                    [::component.config/config :overrides]
                                    {:service {:host            "0.0.0.0"
                                               :port            8080
                                               :allowed-origins ["https://allowed.com" "https://trusted.com"]}}))]

      (is (match? {:status  200
                   :headers {"Access-Control-Allow-Origin" "https://allowed.com"}}
                  (http/get "http://localhost:8080/test"
                            {:headers          {"Origin"        "https://allowed.com"
                                                "authorization" "Bearer test-token"}
                             :throw-exceptions false})))

      (is (match? {:status  403
                   :headers (fn [headers] (nil? (get headers "Access-Control-Allow-Origin")))}
                  (http/get "http://localhost:8080/test"
                            {:headers          {"Origin"        "https://not-allowed.com"
                                                "authorization" "Bearer test-token"}
                             :throw-exceptions false})))

      (ig/halt! system))))
