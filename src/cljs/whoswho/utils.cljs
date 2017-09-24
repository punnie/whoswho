(ns whoswho.utils)

(defn js->clj-kw [ds]
  (js->clj ds :keywordize-keys true))
