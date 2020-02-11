(ns golf-handicap.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [golf-handicap.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[golf-handicap started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[golf-handicap has shut down successfully]=-"))
   :middleware wrap-dev})
