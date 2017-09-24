(ns whoswho.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [whoswho.core-test]))

(doo-tests 'whoswho.core-test)

