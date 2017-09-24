(ns whoswho.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [whoswho.layout :refer [error-page]]
            [whoswho.routes.home :refer [home-routes]]
            [whoswho.routes.api :refer [api-routes]]
            [compojure.route :as route]
            [whoswho.env :refer [defaults]]
            [mount.core :as mount]
            [whoswho.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
   (-> #'home-routes
       (wrap-routes middleware/wrap-csrf))
   (-> #'api-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-restricted)
       (wrap-routes middleware/wrap-formats))
   (-> #'api-routes)
   (route/not-found
    (:body
     (error-page {:status 404
                  :title  "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
