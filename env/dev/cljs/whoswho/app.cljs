(ns ^:figwheel-no-load whoswho.app
  (:require [whoswho.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
