(ns thread-pool-test
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
              :route-name :test]
             ["/blocking" :get [pedestal.service.interceptors/json-body
                                (fn [_]
                                  (Thread/sleep 500)
                                  {:status 200
                                   :body   {:message "done"}})]
              :route-name :blocking]
             ["/thread-info" :get [pedestal.service.interceptors/json-body
                                   (fn [_]
                                     {:status 200
                                      :body   {:virtual? (.isVirtual (Thread/currentThread))}})]
              :route-name :thread-info]])

(def system-components
  {::component.config/config         {:path "test/resources/config.example.edn"
                                      :env  :test}
   ::component.routes/routes         {:routes routes}
   ::component.prometheus/prometheus {:metrics []}
   ::component.service/service       {:components {:config     (ig/ref ::component.config/config)
                                                   :prometheus (ig/ref ::component.prometheus/prometheus)
                                                   :routes     (ig/ref ::component.routes/routes)}}})

(s/deftest thread-pool-configuration-test
  (testing "Default thread pool configuration is applied"
    (let [system (ig/init system-components)]

      ;; Verify service starts and requests work
      (is (match? {:status 200}
                  (http/get "http://localhost:8080/test"
                            {:headers          {"authorization" "Bearer test-token"}
                             :throw-exceptions false})))

      (ig/halt! system))))

(s/deftest custom-thread-pool-configuration-test
  (testing "Custom thread pool configuration can be provided via config"
    (let [system (ig/init (assoc-in system-components
                                    [::component.config/config :overrides]
                                    {:service {:host           "0.0.0.0"
                                               :port           8080
                                               :min-threads    4
                                               :max-threads    16
                                               :max-queue-size 50}}))]

      (is (match? {:status 200}
                  (http/get "http://localhost:8080/test"
                            {:headers          {"authorization" "Bearer test-token"}
                             :throw-exceptions false})))

      (ig/halt! system))))

(s/deftest virtual-threads-enabled-by-default-test
  (testing "Handler threads are virtual when :use-virtual-threads is not set"
    (let [system (ig/init system-components)
          response (http/get "http://localhost:8080/thread-info"
                             {:as               :json
                              :throw-exceptions false})]

      (is (match? {:status 200} response))
      (is (true? (-> response :body :virtual?))
          "Expected handler thread to be a virtual thread")

      (ig/halt! system))))

(s/deftest virtual-threads-disabled-test
  (testing "Handler threads are platform threads when :use-virtual-threads is false"
    (let [system (ig/init (assoc-in system-components
                                    [::component.config/config :overrides]
                                    {:service {:host                "0.0.0.0"
                                               :port                8080
                                               :use-virtual-threads false}}))
          response (http/get "http://localhost:8080/thread-info"
                             {:as               :json
                              :throw-exceptions false})]

      (is (match? {:status 200} response))
      (is (false? (-> response :body :virtual?))
          "Expected handler thread to be a platform thread")

      (ig/halt! system))))

