(ns user
  (:require [mount.core :as mount]
            [whoswho.figwheel :refer [start-fw stop-fw cljs]]
            whoswho.core))

(defn start []
  (mount/start-without #'whoswho.core/repl-server))

(defn stop []
  (mount/stop-except #'whoswho.core/repl-server))

(defn restart []
  (stop)
  (start))


