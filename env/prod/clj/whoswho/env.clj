(ns whoswho.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[whoswho started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[whoswho has shut down successfully]=-"))
   :middleware identity})
