(ns service.interceptors-test
  (:require [clojure.test :refer [is]]
            [io.pedestal.interceptor.chain :as chain]
            [matcher-combinators.test :refer [match?]]
            [schema.core]
            [schema.test :as s]
            [service.interceptors :as interceptors])
  (:import (clojure.lang ExceptionInfo)
           (java.time LocalDate)))

(schema.core/defschema QueryParams
  {:hello schema.core/Str})

(schema.core/defschema QueryParamsOptionalKey
  {(schema.core/optional-key :hello) schema.core/Str})

(schema.core/defschema QueryParamsWithDate
  {(schema.core/optional-key :reference-date) LocalDate})

(s/deftest query-params-schema-test
  (let [ex (is (thrown? ExceptionInfo (chain/execute {} [(interceptors/query-params-schema QueryParams)])))]
    (is (match? {:status  422
                 :error   "invalid-payload-for-query-params"
                 :message "The system detected that the received data is invalid."
                 :detail  {:hello "Missing required key"}}
                (ex-data ex))))

  (is (match? {:request {:query-params {:hello "world"}}}
              (chain/execute {:request {:query-params {:hello "world"}}} [(interceptors/query-params-schema QueryParams)])))

  (is (match? {:request {:query-params {}}}
              (chain/execute {:request {}} [(interceptors/query-params-schema QueryParamsOptionalKey)])))

  (is (match? {:request {:query-params {:reference-date (LocalDate/of 1998 12 26)}}}
              (chain/execute {:request {:query-params {:reference-date "1998-12-26"}}} [(interceptors/query-params-schema QueryParamsWithDate)]))))

(schema.core/defschema EqKeywordSchema
  {:type (schema.core/eq :hello-world)})

(s/deftest wire-in-body-schema-test
  (is (match? {:request {:json-params {:type :hello-world}}}
              (chain/execute {:request {:json-params {:type "hello-world"}}} [(interceptors/wire-in-body-schema EqKeywordSchema)])))

  (let [ex (is (thrown? ExceptionInfo (chain/execute {:request {:json-params {:type "test"}}} [(interceptors/wire-in-body-schema EqKeywordSchema)])))]
    (is (match? {:status  422
                 :error   "invalid-request-body-payload"
                 :message "The system detected that the received data is invalid."
                 :detail  "{:type [not [= :hello-world :test]]}"}
                (ex-data ex)))))
