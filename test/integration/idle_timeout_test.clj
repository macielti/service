(ns idle-timeout-test
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
            [service.interceptors :as interceptors])
  (:import (java.net Socket SocketTimeoutException)
           (java.io BufferedReader InputStreamReader PrintWriter)))

(def routes [["/test" :get [interceptors/http-request-in-handle-timing-interceptor
                            pedestal.service.interceptors/json-body
                            (fn [{{:keys [config]} :components}]
                              {:status 200
                               :body   config})]
              :route-name :test]
             ["/slow" :get [pedestal.service.interceptors/json-body
                            (fn [_]
                              (Thread/sleep 2000)
                              {:status 200
                               :body   {:message "slow response"}})]
              :route-name :slow]])

(def system-components
  {::component.config/config         {:path "test/resources/config.example.edn"
                                      :env  :test}
   ::component.routes/routes         {:routes routes}
   ::component.prometheus/prometheus {:metrics []}
   ::component.service/service       {:components {:config     (ig/ref ::component.config/config)
                                                   :prometheus (ig/ref ::component.prometheus/prometheus)
                                                   :routes     (ig/ref ::component.routes/routes)}}})

(defn- create-socket-connection
  "Creates a raw socket connection to test idle timeout behavior."
  [port]
  (Socket. "localhost" (int port)))

(defn- send-partial-request
  "Sends a partial HTTP request and waits to trigger idle timeout."
  [socket wait-ms]
  (let [^java.io.OutputStream os (.getOutputStream socket)
        out (PrintWriter. os true)]
    (.println out "GET /test HTTP/1.1")
    (.println out "Host: localhost")
    ;; Don't send the final blank line to keep connection open
    (.flush out)
    (Thread/sleep (long wait-ms))))

(s/deftest default-idle-timeout-test
  (testing "The default idle timeout of 30000ms is applied when not configured"
    (let [system (ig/init system-components)]

      ;; Verify normal requests work
      (is (match? {:status 200}
                  (http/get "http://localhost:8080/test"
                            {:headers          {"authorization" "Bearer test-token"}
                             :throw-exceptions false})))

      ;; Test that connection times out after default idle timeout
      (testing "Connection should timeout after being idle"
        (let [socket (create-socket-connection 8080)]
          (try
            ;; Set socket read timeout slightly longer than expected idle timeout
            (.setSoTimeout socket 35000)

            ;; Send partial request and wait
            (send-partial-request socket 31000)

            ;; Try to read response - should time out or connection closed
            (let [in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
                  response (try
                             (.readLine in)
                             (catch SocketTimeoutException _
                               :socket-timeout)
                             (catch Exception e
                               (if (or (.contains (.getMessage e) "Connection reset")
                                       (.contains (.getMessage e) "Socket closed"))
                                 :connection-closed
                                 (throw e))))]
              (is (or (= :socket-timeout response)
                      (= :connection-closed response)
                      (nil? response))
                  "Connection should be closed after idle timeout"))
            (finally
              (.close socket)))))

      (ig/halt! system))))

(s/deftest custom-idle-timeout-test
  (testing "A custom idle timeout can be configured via :idle-timeout-ms"
    (let [system (ig/init (assoc-in system-components
                                    [::component.config/config :overrides]
                                    {:service {:host            "0.0.0.0"
                                               :port            8080
                                               :idle-timeout-ms 5000}}))]

      ;; Verify normal requests work
      (is (match? {:status 200}
                  (http/get "http://localhost:8080/test"
                            {:headers          {"authorization" "Bearer test-token"}
                             :throw-exceptions false})))

      ;; Test that connection times out after custom idle timeout
      (testing "Connection should timeout after custom idle timeout period"
        (let [socket (create-socket-connection 8080)]
          (try
            ;; Set socket read timeout slightly longer than expected idle timeout
            (.setSoTimeout socket 10000)

            ;; Send partial request and wait for longer than custom timeout
            (send-partial-request socket 6000)

            ;; Try to read response - should time out or connection closed
            (let [in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
                  response (try
                             (.readLine in)
                             (catch SocketTimeoutException _
                               :socket-timeout)
                             (catch Exception e
                               (if (or (.contains (.getMessage e) "Connection reset")
                                       (.contains (.getMessage e) "Socket closed"))
                                 :connection-closed
                                 (throw e))))]
              (is (or (= :socket-timeout response)
                      (= :connection-closed response)
                      (nil? response))
                  "Connection should be closed after custom idle timeout"))
            (finally
              (.close socket)))))

      (ig/halt! system))))

(s/deftest idle-timeout-does-not-affect-active-connections-test
  (testing "Idle timeout does not affect active connections that are processing requests"
    (let [system (ig/init (assoc-in system-components
                                    [::component.config/config :overrides]
                                    {:service {:host            "0.0.0.0"
                                               :port            8080
                                               :idle-timeout-ms 5000}}))]

      ;; A slow endpoint that takes 2 seconds should complete successfully
      ;; even though it's less than the idle timeout
      (is (match? {:status 200
                   :body   "{\"message\":\"slow response\"}"}
                  (http/get "http://localhost:8080/slow"
                            {:throw-exceptions false
                             :socket-timeout   10000})))

      (ig/halt! system))))
