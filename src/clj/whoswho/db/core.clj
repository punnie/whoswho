(ns whoswho.db.core
    (:require [monger.core :as mg]
              [monger.collection :as mc]
              [monger.operators :refer :all]
              [mount.core :refer [defstate]]
              [whoswho.config :refer [env]]))

(defstate db*
  :start (-> env :database-url mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

(defn upsert-user [slack-id user]
  (mc/update db "users" {:id slack-id} {$set user} {:upsert true}))

(defn upsert-team [slack-id team]
  (mc/update db "teams" {:id slack-id} {$set team} {:upsert true}))

(defn find-user-by-id [id]
  (mc/find-one-as-map db "users" {:_id id}))

(defn find-user-by-slack-id [id]
  (mc/find-one-as-map db "users" {:id id}))

(defn find-team-by-slack-id [id]
  (mc/find-one-as-map db "teams" {:id id}))

(defn find-user-by-slack-token [token]
  (mc/find-one-as-map db "users" {:access_token token}))
