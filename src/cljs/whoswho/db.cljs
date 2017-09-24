(ns whoswho.db
  (:require [reagent.core :refer [atom]]))

(def db (atom {:current-user nil
               :current-team nil
               :team-members []}))
