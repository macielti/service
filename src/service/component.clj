(ns service.component
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [io.pedestal.connector]
            [io.pedestal.http.http-kit :as http-kit]))

(defmethod ig/init-key ::service
  [_ {:keys [components]}]
  (log/info :starting ::service)
  (let [service-config (-> components :config :service)
        connector      (-> {:host            (:host service-config)
                            :port            (:port service-config)
                            :router          :sawtooth
                            :initial-context {}
                            :join?           false}
                           (io.pedestal.connector/with-routes (:routes components))
                           (http-kit/create-connector (select-keys service-config [:worker-threads])))]
    (io.pedestal.connector/start! connector)))

(defmethod ig/halt-key! ::service
  [_ service]
  (log/info :stopping ::service)
  (io.pedestal.connector/stop! service))
