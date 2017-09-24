(ns whoswho.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [whoswho.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[whoswho started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[whoswho has shut down successfully]=-"))
   :middleware wrap-dev})
