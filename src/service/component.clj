(ns service.component
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [io.pedestal.connector]
            [io.pedestal.http.jetty :as jetty]
            [service.interceptors :as io.interceptors])
  (:import (org.eclipse.jetty.server Server)
           (org.eclipse.jetty.util BlockingArrayQueue)
           (org.eclipse.jetty.util.thread QueuedThreadPool VirtualThreadPool)))

(def ^:private default-idle-timeout-ms 30000)
(def ^:private default-min-threads 8)
(def ^:private default-max-threads 50)
(def ^:private default-max-queue-size 200)

(def ^:private allow-all-origins (constantly true))

(defn- java-version []
  (let [version (System/getProperty "java.version")
        major-version (Integer/parseInt (first (str/split version #"\.")))]
    major-version))

(defn- supports-virtual-threads? []
  (>= (java-version) 21))

(defn- build-thread-pool [min-threads max-threads max-queue-size use-virtual-threads]
  (if (and use-virtual-threads (supports-virtual-threads?))
    (do
      (log/info :using-virtual-threads true)
      (VirtualThreadPool. max-threads))
    (do
      (when use-virtual-threads
        (log/warn :virtual-threads-requested-but-not-supported (java-version)))
      (QueuedThreadPool. max-threads
                         min-threads
                         60000
                         (BlockingArrayQueue. max-queue-size)))))

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
  (let [service-config (-> components :config :service)
        idle-timeout (or (:idle-timeout-ms service-config) default-idle-timeout-ms)
        min-threads (or (:min-threads service-config) default-min-threads)
        max-threads (or (:max-threads service-config) default-max-threads)
        max-queue-size (or (:max-queue-size service-config) default-max-queue-size)
        use-virtual-threads (if (contains? service-config :use-virtual-threads)
                              (:use-virtual-threads service-config)
                              true)
        allowed-origins' (:allowed-origins service-config)
        connector (-> {:host            (:host service-config)
                       :port            (:port service-config)
                       :type            :jetty
                       :router          :sawtooth
                       :initial-context {}
                       :join?           false}
                      (io.pedestal.connector/with-default-interceptors :allowed-origins (allowed-origins allowed-origins'))
                      (io.pedestal.connector/with-interceptors [io.interceptors/error-handler-interceptor
                                                                (io.interceptors/components-interceptor components)])
                      (io.pedestal.connector/with-routes (:routes components))
                      (jetty/create-connector {:container-options
                                               {:thread-pool (build-thread-pool min-threads max-threads max-queue-size use-virtual-threads)
                                                :configurator (fn [^Server server]
                                                                (doseq [connector (.getConnectors server)]
                                                                  (.setIdleTimeout connector idle-timeout))
                                                                server)}}))]
    (io.pedestal.connector/start! connector)))

(defmethod ig/halt-key! ::service
  [_ service]
  (log/info :stopping ::service)
  (io.pedestal.connector/stop! service))
