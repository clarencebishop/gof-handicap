(ns golf-handicap.routes.home
  (:require
   [golf-handicap.layout :as layout]
   [golf-handicap.db.core :as db]
   [clojure.java.io :as io]
   [golf-handicap.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [struct.core :as st]))

(def score-schema
  [[:golfer_name
    st/required
    st/string]
   
   [:course_name
    st/required
    st/string]
   
   [:date_played
    st/required
    st/string]
   
   [:rating
    st/required
    st/string]

   [:slope
    st/required
    st/string]

   [:score
    st/required
    st/string]])


(defn validate-score [params]
  (print params)
  (first (st/validate params score-schema)))

(defn save-score! [{:keys [params]}]
  (if-let [errors (validate-score params)]
    (-> (response/found "/")
        (assoc :flash (assoc params :errors errors)))
    (do
      (db/save-score! params) 
      (response/found "/"))))


(defn home-page [{:keys [flash] :as request}]
  (layout/render
   request
   "home.html"
   (merge {:scores (db/get-all-scores)}
          (select-keys flash [:golfer_name :course_name :date_played :rating :slope :score :errors]))))

(defn index-page [request]
  (layout/render
    request
    "scores.html"
    {:scores (db/get-all-scores)}))

(defn handicap-page [request]
  (layout/render
    request
    "handicaps.html"
    {:handicaps [{:name "Clarence Bishop" :handicap 9.9}
                 {:name "Ty Elliott" :handicap 6.7}
                 {:name "Eric Williams" :handicap 12.8}
                 {:name "Kevin Posney" :handicap 11.4}]}))

(defn about-page [request]
  (layout/render request "about.html"))


(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page
         :post save-score!}]
   ["/scores" {:get index-page}]
   ["/handicaps" {:get handicap-page}]
   ["/about" {:get about-page}]])

