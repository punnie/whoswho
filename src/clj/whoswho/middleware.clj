(ns whoswho.middleware
  (:require [whoswho.env :refer [defaults]]
            [cognitect.transit :as transit]
            [clojure.tools.logging :as log]
            [whoswho.layout :refer [*app-context* error-page]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [whoswho.oauth2 :as oauth2]
            [muuntaja.core :as muuntaja]
            [muuntaja.format.transit :as transit-format]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [whoswho.config :refer [env]]
            [whoswho.oauth2-user :refer [handle-data-from-oauth2]]
            [whoswho.db.core :refer [find-user-by-slack-token]]
            [ring-ttl-session.core :refer [ttl-memory-store]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]])
  (:import [javax.servlet ServletContext]
           [org.joda.time ReadableInstant]))

(defn wrap-context [handler]
  (fn [request]
    (binding [*app-context*
              (if-let [context (:servlet-context request)]
                ;; If we're not inside a servlet environment
                ;; (for example when using mock requests), then
                ;; .getContextPath might not exist
                (try (.getContextPath ^ServletContext context)
                     (catch IllegalArgumentException _ context))
                ;; if the context is not specified in the request
                ;; we check if one has been specified in the environment
                ;; instead
                (:app-context env))]
      (handler request))))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t)
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))

(def joda-time-writer
  (transit/write-handler
    (constantly "m")
    (fn [v] (-> ^ReadableInstant v .getMillis))
    (fn [v] (-> ^ReadableInstant v .getMillis .toString))))

(def restful-format-options
  (update
    muuntaja/default-options
    :formats
    merge
    {"application/transit+json"
     {:decoder [(partial transit-format/make-transit-decoder :json)]
      :encoder [#(transit-format/make-transit-encoder
                   :json
                   (merge
                     %
                     {:handlers {org.joda.time.DateTime joda-time-writer}}))]}}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format restful-format-options))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn on-error [request response]
  (error-page
    {:status 403
     :title (str "Access to " (:uri request) " is not authorized")}))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-oauth2 [handler]
  (oauth2/wrap-oauth2 handler
                      {:slack-team
                       {:authorize-uri    (env :authorize-uri)
                        :access-token-uri (env :access-token-uri)
                        :client-id        (env :client-id)
                        :client-secret    (env :client-secret)
                        :scopes           ["users:read"
                                           "users.profile:read"
                                           "team:read"]
                        :launch-uri       "/oauth2/slack/team"
                        :redirect-uri     "/oauth2/slack/callback"
                        :landing-uri      "/"}

                       :slack-user
                       {:authorize-uri    (env :authorize-uri)
                        :access-token-uri (env :access-token-uri)
                        :client-id        (env :client-id)
                        :client-secret    (env :client-secret)
                        :scopes           ["identity.basic"
                                           "identity.team"
                                           "identity.avatar"]
                        :launch-uri       "/oauth2/slack"
                        :redirect-uri     "/oauth2/slack/callback"
                        :landing-uri      "/"}}

                      {:data-callback handle-data-from-oauth2}))

(defn wrap-current-user [handler]
  (fn [req]
    (if-let [slack-token (get-in req [:session :whoswho.oauth2/access-tokens :slack-user :token])]
      (if-let [user (find-user-by-slack-token slack-token)]
        (handler (assoc-in req [:session :identity] (:_id user))))
      (handler req))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      wrap-current-user
      wrap-oauth2
      wrap-webjars
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in [:session :store] (cookie-store {:key "aeph9Ieph1feipie"}))
            (assoc-in [:session :cookie-attrs :max-age] (* 60 60 24))
            (assoc-in [:session :cookie-attrs :same-site] :lax)))
      wrap-context
      wrap-internal-error))
