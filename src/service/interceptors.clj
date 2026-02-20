(ns service.interceptors
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [humanize.schema :as h]
            [iapetos.core :as prometheus]
            [io.pedestal.interceptor :as pedestal.interceptor]
            [io.pedestal.interceptor.error :as error]
            [schema.coerce :as coerce]
            [schema.core]
            [schema.utils]
            [service.error :as common-error])
  (:import (java.time LocalDate)))

(def error-handler-interceptor
  (error/error-dispatch [ctx ex]
                        [{:exception-type :clojure.lang.ExceptionInfo}]
                        (let [{:keys [status error message detail]} (ex-data ex)]
                          (assoc ctx :response {:status  status
                                                :headers {"Content-Type" "application/json;charset=UTF-8"}
                                                :body    (json/encode {:error   error
                                                                       :message message
                                                                       :detail  detail})}))

                        :else
                        (do (log/error ex)
                            (assoc ctx :response {:status  500
                                                  :headers {"Content-Type" "application/json;charset=UTF-8"}
                                                  :body    (json/encode {:error   "unexpected-server-error"
                                                                         :message "Internal Server Error"
                                                                         :detail  "Internal Server Error"})}))))

(defn components-interceptor [system-components]
  (pedestal.interceptor/interceptor
   {:name  ::components-interceptor
    :enter (fn [context]
             (assoc-in context [:request :components] system-components))}))

(defn ^:private json-matcher
  [schema]
  (or (when (and (instance? schema.core.EqSchema schema)
                 (keyword? (:v schema)))
        (fn [x] (if (string? x) (keyword x) x)))
      (coerce/json-coercion-matcher schema)))

(defn wire-in-body-schema [schema]
  (pedestal.interceptor/interceptor
   {:name  ::schema-body-in-interceptor
    :enter (fn [{{:keys [json-params]} :request :as context}]
             (let [coercer-fn (coerce/coercer schema json-matcher)
                   coercion-result (coercer-fn json-params)]
               (when (schema.utils/error? coercion-result)
                 (common-error/http-friendly-exception 422
                                                       "invalid-request-body-payload"
                                                       "The system detected that the received data is invalid."
                                                       (-> (schema.utils/error-val coercion-result) h/explain str)))
               (assoc-in context [:request :json-params] coercion-result)))}))

(def http-request-in-handle-timing-interceptor
  (pedestal.interceptor/interceptor
   {:name  ::http-request-in-handle-timing
    :enter (fn [context]
             (assoc context ::start-ms (System/currentTimeMillis)))
    :leave (fn [{{:keys [components]} :request :as context}]
             (let [{::keys [start-ms]} context
                   prometheus-registry (get-in components [:prometheus :registry])
                   service-name (get-in components [:config :service-name])
                   elapsed-ms (- (System/currentTimeMillis) start-ms)
                   route-name (get-in context [:route :route-name])]
               (prometheus/observe prometheus-registry :http-request-in-handle-timing-v2
                                   {:service  (name service-name)
                                    :endpoint (name route-name)}
                                   elapsed-ms)
               (dissoc context ::start-ms)))}))

(def ^:private coercions
  {LocalDate (fn [input] (LocalDate/parse input))})

(defn ^:private coercions-matcher
  [schema]
  (coercions schema))

(defn query-params-schema [schema]
  (pedestal.interceptor/interceptor
   {:name  ::query-params-schema
    :enter (fn [{{:keys [query-params]} :request :as context}]
             (let [coercer-fn (coerce/coercer schema coercions-matcher)
                   query-params' (coercer-fn (or query-params {}))]
               (when (schema.utils/error? query-params')
                 (common-error/http-friendly-exception 422
                                                       "invalid-payload-for-query-params"
                                                       "The system detected that the received data is invalid."
                                                       (-> (schema.utils/error-val query-params') h/explain)))
               (assoc-in context [:request :query-params] query-params')))}))
