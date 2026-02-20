(ns service.component
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [io.pedestal.connector]
            [io.pedestal.http.jetty :as jetty]
            [service.interceptors :as io.interceptors]))

(defmethod ig/init-key ::service
  [_ {:keys [components]}]
  (log/info :starting ::service)
  (let [connector (-> {:host            (-> components :config :service :host)
                       :port            (-> components :config :service :port)
                       :type            :jetty
                       :router          :sawtooth
                       :initial-context {}
                       :join?           false}
                      (io.pedestal.connector/with-default-interceptors :allowed-origins (constantly true))
                      (io.pedestal.connector/with-interceptors [io.interceptors/error-handler-interceptor
                                                                (io.interceptors/components-interceptor components)])
                      (io.pedestal.connector/with-routes (:routes components))
                      (jetty/create-connector {}))]
    (io.pedestal.connector/start! connector)))

(defmethod ig/halt-key! ::service
  [_ service]
  (log/info :stopping ::service)
  (io.pedestal.connector/stop! service))
