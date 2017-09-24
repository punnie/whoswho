(ns whoswho.routes.home
  (:require [whoswho.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as http-response]
            [ring.util.response :refer [redirect]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(defn home-page [req]
  (if-let [token (get-in req [:session :whoswho.oauth2/access-tokens :slack :token])]
    (layout/render "home.html" {:current-user {:token token}})
    (layout/render "home.html" {:current-user nil})))

(defn logout []
  (assoc (redirect "/") :session nil))

(defroutes home-routes
  (GET "/" []
       home-page)
  (GET "/logout" []
       (logout))
  (GET "/docs" []
       (-> (http-response/ok (-> "docs/docs.md" io/resource slurp))
           (http-response/header "Content-Type" "text/plain; charset=utf-8"))))
