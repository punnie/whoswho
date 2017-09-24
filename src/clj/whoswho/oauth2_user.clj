(ns whoswho.oauth2-user
  (:require [clojure.set :refer [union]]
            [clojure.string :refer [split]]
            [clojure.tools.logging :as log]
            [whoswho.db.core :refer [find-team-by-slack-id upsert-team upsert-user]]))

(defn handle-data-from-oauth2
  [{:keys [ok access_token scope user user_id team team_id] :as oauth2-data}]
  (log/info oauth2-data)
  (if ok
    (if user
      (let [team-id (team :id)
            user-id (user :id)
            user    (assoc user :access_token access_token :scope (split scope #",") :team_id team-id)]
        (upsert-user user-id user)
        (upsert-team team-id team))
      (let [team-id       team_id
            team          (find-team-by-slack-id team-id)
            old-scopes    (set (or (team :scopes) []))
            oauth2-scopes (set (split scope #","))
            new-scopes    (union old-scopes oauth2-scopes)
            new-team      (assoc team :scope new-scopes :access_token access_token)]
        (upsert-team team-id new-team)))))
