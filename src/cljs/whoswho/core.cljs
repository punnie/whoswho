(ns whoswho.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [whoswho.ajax :refer [load-interceptors!]]
            [whoswho.db :refer [db]]
            [whoswho.utils :refer [js->clj-kw]]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href uri
     :on-click #(reset! collapsed? true)} title]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-dark.bg-primary
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "whoswho"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

(defn team-component []
  (fn []
    (if-let [team (get-in @db [:team-members])]
      [:div (for [member team]
              [:img {:src (get-in member [:profile :image_512])}])]
      [:p [:a {:href "/oauth2/slack/team"} "Add your team"]])))

(defn home-page []
  (fn []
    (let [current-user      (get-in @db [:current-user])
          current-user-name (get-in current-user [:name])]
      [:div.container
         [:div.row>div.col-sm-12
          [:div
           (if current-user
             [:div
              [:p "Welcome " current-user-name "! "
               [:a {:href "/logout"} "Logout"]]
              [team-component]]
             [:a {:href "/oauth2/slack"} "Sign in with Slack"])]]])))

(def pages
  {:home #'home-page
   :about #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn load-database! []
  (swap! db assoc-in [:current-user] (-> js/window
                                         (aget "currentUser")
                                         (js->clj-kw)))
  (swap! db assoc-in [:current-team] (-> js/window
                                         (aget "currentTeam")
                                         (js->clj-kw))))

(defn fetch-team-members! []
  (GET "/api/team/members.json" {:handler #(swap! db assoc-in [:team-members] %)
                                 :error-handler   #(swap! db assoc-in [:team-members] nil)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (load-database!)
  (fetch-team-members!)
  (hook-browser-navigation!)
  (mount-components))
