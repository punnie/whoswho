(ns whoswho.db
  (:require [reagent.core :refer [atom]]))

(def db (atom {:current-user nil}))
