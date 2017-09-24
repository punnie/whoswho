(ns whoswho.oauth2-user
  (:require [clojure.tools.logging :as log]
            [whoswho.db.core :refer [upsert-user upsert-team]]))

(defn handle-user-data-from-oauth2
  [{:keys [ok access_token scope user team] :as oauth2-data}]
  (if ok
    (let [user    (assoc user :access_token access_token)
          user-id (user :id)
          team-id (team :id)]
      (upsert-user user-id user)
      (upsert-team team-id team))))
