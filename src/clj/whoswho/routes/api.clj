(ns whoswho.routes.api
  (:require [clj-slack.users :as slack-users]
            [clj-http.client :as http]
            [compojure.core :refer [context defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [whoswho.db.core :refer [find-team-by-slack-id find-user-by-id]]
            [clojure.tools.logging :as log]))

(defn sync-team [req]
  (if-let [current-user-id (get-in req [:session :identity])]
    (let [current-user          (find-user-by-id current-user-id)
          current-team-slack-id (current-user :team_id)
          current-team          (find-team-by-slack-id current-team-slack-id)]
        (ok (:members current-team)))))

(defn human? [member]
  (and (not= (member :id) "USLACKBOT") (not (member :is_bot))))

(defn still-alive? [member]
  (not (member :deleted)))

(def ^:dynamic *ua* {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"})

(defn not-boring? [member]
  (try
    (let [http-response (http/head (get-in member [:profile :image_512]) {:headers *ua*})
          last-redirect (-> http-response :trace-redirects last)]
      (log/info (-> http-response :trace-redirects))
      (or (nil? last-redirect)))
    (catch Exception e
      true)))

(defn filter-lame-slack-users [members]
  (->> members
       (filter human?)
       (filter still-alive?)
       (filter not-boring?)))

(defn get-team [req]
  (if-let [current-user-id (get-in req [:session :identity])]
    (let [current-user          (find-user-by-id current-user-id)
          current-team-slack-id (current-user :team_id)
          current-team          (find-team-by-slack-id current-team-slack-id)
          token                 (current-team :access_token)
          connection            {:api-url "https://slack.com/api" :token token}
          users                 (slack-users/list connection)]
      (if (:ok users)
        (let [members          (:members users)
              filtered-members (filter-lame-slack-users (:members users))]
          (ok filtered-members))))))

(defroutes api-routes
  (context "/api" []
           (GET  "/team/members.json" [] get-team)
           (POST "/team/sync.json"    [] sync-team)))
