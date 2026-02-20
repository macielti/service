(ns component-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [is testing]]
            [common-clj.integrant-components.config :as component.config]
            [common-clj.integrant-components.prometheus :as component.prometheus]
            [common-clj.integrant-components.routes :as component.routes]
            [iapetos.export :as export]
            [integrant.core :as ig]
            [io.pedestal.connector.test :as test]
            [io.pedestal.service.interceptors :as pedestal.service.interceptors]
            [matcher-combinators.test :refer [match?]]
            [schema.core :as schema]
            [schema.test :as s]
            [service.component :as component.service]
            [service.interceptors :as interceptors]))

(def test-state (atom nil))

(def routes [["/test" :get [interceptors/http-request-in-handle-timing-interceptor
                            pedestal.service.interceptors/json-body
                            (fn [{{:keys [config]} :components}]
                              {:status 200
                               :body   config})]
              :route-name :test]
             ["/schema-validation-interceptor-test" :post [(interceptors/wire-in-body-schema {:test                       schema/Str
                                                                                              (schema/optional-key :type) schema/Keyword})
                                                           pedestal.service.interceptors/json-body
                                                           (fn [{:keys [json-params]}]
                                                             (reset! test-state json-params)
                                                             {:status 200
                                                              :body   {:test :schema-ok}})]
              :route-name :schema-validation-interceptor-test]])

(def system-components
  {::component.config/config         {:path "test/resources/config.example.edn"
                                      :env  :test}
   ::component.routes/routes         {:routes routes}
   ::component.prometheus/prometheus {:metrics []}
   ::component.service/service       {:components {:config     (ig/ref ::component.config/config)
                                                   :prometheus (ig/ref ::component.prometheus/prometheus)
                                                   :routes     (ig/ref ::component.routes/routes)}}})

(s/deftest component-test
  (let [system (ig/init system-components)
        connector (-> system ::component.service/service)
        prometheus-registry (-> system ::component.prometheus/prometheus :registry)]

    (testing "That we can fetch the test endpoint and access components from the request"
      (is (match? {:status  200
                   :headers {"Content-Type" "application/json;charset=UTF-8"}
                   :body    "{\"service-name\":\"rango\",\"service\":{\"host\":\"0.0.0.0\",\"port\":8080},\"current-env\":\"test\"}"}
                  (test/response-for connector :get "/test" :headers {"authorization" "Bearer test-token"}))))

    (testing "That we can't fetch the test endpoint without a valid schema"
      (reset! test-state nil)
      (is (match? {:status  422
                   :headers {"Content-Type" "application/json;charset=UTF-8"}
                   :body    "{\"error\":\"invalid-request-body-payload\",\"message\":\"The system detected that the received data is invalid.\",\"detail\":\"{:test \\\"Missing required key\\\"}\"}"}
                  (test/response-for connector :post "/schema-validation-interceptor-test"
                                     :headers {:content-type "application/json"}
                                     :body (json/encode {}))))

      (is (match? {:status  422
                   :headers {"Content-Type" "application/json;charset=UTF-8"}
                   :body    "{\"error\":\"invalid-request-body-payload\",\"message\":\"The system detected that the received data is invalid.\",\"detail\":\"{:hello \\\"Invalid key.\\\", :test \\\"Missing required key\\\"}\"}"}
                  (test/response-for connector :post "/schema-validation-interceptor-test"
                                     :headers {:content-type "application/json"}
                                     :body (json/encode {:hello :world}))))

      (reset! test-state nil)
      (is (match? {:status  422
                   :headers {"Content-Type" "application/json;charset=UTF-8"}
                   :body    "{\"error\":\"invalid-request-body-payload\",\"message\":\"The system detected that the received data is invalid.\",\"detail\":\"The value must be a map, but was '' instead.\"}"}
                  (test/response-for connector :post "/schema-validation-interceptor-test")))

      (reset! test-state nil)
      (is (match? {:status 200
                   :body   "{\"test\":\"schema-ok\"}"}
                  (test/response-for connector :post "/schema-validation-interceptor-test"
                                     :headers {:content-type "application/json"}
                                     :body (json/encode {:test :ok}))))
      (is (= {:test "ok"}
             @test-state))

      (reset! test-state nil)
      (is (match? {:status 200
                   :body   "{\"test\":\"schema-ok\"}"}
                  (test/response-for connector :post "/schema-validation-interceptor-test"
                                     :headers {:content-type "application/json"}
                                     :body (json/encode {:test :ok
                                                         :type :simple-test})))))
    (is (= {:test "ok"
            :type :simple-test}
           @test-state))

    (is (str/includes? (export/text-format prometheus-registry) "http_request_in_handle_timing_v2_sum{service=\"rango\",endpoint=\"test\",}"))

    (ig/halt! system)))
