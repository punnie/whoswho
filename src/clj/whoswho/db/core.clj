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

(defn upsert-user [id user]
  (mc/upsert db "users" {:id id} user))

(defn upsert-team [id team]
  (mc/upsert db "teams" {:id id} team))
