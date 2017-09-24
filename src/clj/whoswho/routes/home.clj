(ns whoswho.routes.home
  (:require [whoswho.layout :as layout]
            [whoswho.db.core :refer [find-user-by-id find-team-by-slack-id]]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as http-response]
            [ring.util.response :refer [redirect]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(defn home-page [req]
  (log/info req)
  (if-let [current-user-id (get-in req [:session :identity])]
    (let [current-user          (find-user-by-id current-user-id)
          current-team-slack-id (current-user :team_id)
          current-team          (find-team-by-slack-id current-team-slack-id)]
      (layout/render "home.html" {:current-user (dissoc current-user :_id)
                                  :current-team (dissoc current-team :_id)}))
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
