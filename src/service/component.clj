(ns service.component
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [io.pedestal.connector]
            [io.pedestal.http.jetty :as jetty]
            [service.interceptors :as io.interceptors]))

(def ^:private default-idle-timeout-ms 30000)

(def ^:private allow-all-origins (constantly true))

(defn ^:private allowed-origins
  "Returns the allowed-origins value for Pedestal.
  When a collection of origin strings is provided, returns it as a set.
  Otherwise, allows all origins."
  [origins]
  (if (not-empty origins)
    (set origins)
    allow-all-origins))

(defmethod ig/init-key ::service
  [_ {:keys [components]}]
  (log/info :starting ::service)
  (let [idle-timeout (or (-> components :config :service :idle-timeout-ms)
                         default-idle-timeout-ms)
        allowed-origins' (-> components :config :service :allowed-origins)
        connector (-> {:host            (-> components :config :service :host)
                       :port            (-> components :config :service :port)
                       :type            :jetty
                       :router          :sawtooth
                       :initial-context {}
                       :join?           false}
                      (io.pedestal.connector/with-default-interceptors :allowed-origins (allowed-origins allowed-origins'))
                      (io.pedestal.connector/with-interceptors [io.interceptors/error-handler-interceptor
                                                                (io.interceptors/components-interceptor components)])
                      (io.pedestal.connector/with-routes (:routes components))
                      (jetty/create-connector {:io.pedestal.http.jetty/idle-timeout idle-timeout}))]
    (io.pedestal.connector/start! connector)))

(defmethod ig/halt-key! ::service
  [_ service]
  (log/info :stopping ::service)
  (io.pedestal.connector/stop! service))
