(ns component-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [is testing]]
            [common-clj.integrant-components.config :as component.config]
            [common-clj.integrant-components.prometheus :as component.prometheus]
            [common-clj.integrant-components.routes :as component.routes]
            [integrant.core :as ig]
            [io.pedestal.connector.test :as test]
            [io.pedestal.interceptor :as pedestal.interceptor]
            [matcher-combinators.test :refer [match?]]
            [schema.core :as schema]
            [schema.test :as s]
            [service.component :as component.service]
            [service.interceptors :as interceptors]))

(def test-state (atom nil))

(def parse-json-body-interceptor
  (pedestal.interceptor/interceptor
   {:name ::parse-json-body
    :enter (fn [ctx]
             (let [body (-> ctx :request :body)
                   body-str (if body
                              (slurp body)
                              "")]
               (assoc-in ctx [:request :json-params]
                         (if (str/blank? body-str)
                           ""
                           (json/decode body-str true)))))}))

(def routes [["/test" :get [(fn [_]
                              {:status  200
                               :headers {"Content-Type" "application/json;charset=UTF-8"}
                               :body    "ok"})]
              :route-name :test]
             ["/schema-validation-interceptor-test" :post [interceptors/error-handler-interceptor
                                                            parse-json-body-interceptor
                                                            (interceptors/wire-in-body-schema {:test                       schema/Str
                                                                                               (schema/optional-key :type) schema/Keyword})
                                                            (fn [{:keys [json-params]}]
                                                              (reset! test-state json-params)
                                                              {:status  200
                                                               :headers {"Content-Type" "application/json;charset=UTF-8"}
                                                               :body    (json/encode {:test :schema-ok})})]
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
  (let [system    (ig/init system-components)
        connector (-> system ::component.service/service)]

    (testing "That we can fetch the test endpoint"
      (is (match? {:status  200
                   :headers {"Content-Type" "application/json;charset=UTF-8"}
                   :body    "ok"}
                  (test/response-for connector :get "/test"))))

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
                                                         :type :simple-test}))))
      (is (= {:test "ok"
              :type :simple-test}
             @test-state)))

    (ig/halt! system)))
