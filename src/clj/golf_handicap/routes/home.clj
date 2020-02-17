(ns golf-handicap.routes.home
  (:require
   [golf-handicap.layout :as layout]
   [golf-handicap.db.core :as db]
   [clojure.java.io :as io]
   [golf-handicap.routes.auth :as auth]
   [ring.util.response]
   [ring.util.http-response :as response]
   [struct.core :as st]))

(def score-schema
  [[:golfer_name st/required st/string]
   [:course_name st/required st/string]
   [:date_played st/required st/string]
   [:rating st/required st/number-str]
   [:slope st/required st/integer-str]
   [:score st/required st/integer-str]])


(defn validate-score [params]
  (st/validate params score-schema))

(defn save-score! [{:keys [params]}]
  (let [result (validate-score params) error (first result) p (second result)]
       (if (first result)
         (-> (response/found "/")
             (assoc :flash (assoc params :errors error)))
         (do
           (db/save-score! p)
           (response/found "/")))))

(def user-schema
  [[:name st/required st/string]
   [:email st/required st/string]
   [:password st/required st/string]])


(defn validate-user [params]
  (st/validate params user-schema))

(defn create-user! [{:keys [params]}]
  (let [result (validate-user params) error (first result) p (second result)]
       (if (first result)
         (-> (response/found "/")
             (assoc :flash (assoc params :errors error)))
         (do
           (db/create-user! p)
           (response/found "/")))))

(defn home-page [{:keys [flash] :as request}]
  (layout/render
   request
   "home.html"
   (merge {:scores (db/get-all-scores)}
          (select-keys flash [:golfer_name :course_name :date_played :rating :slope :score :errors]))))

(defn register-page [{:keys [flash] :as request}]
  (layout/render
   request
   "register.html"
   (merge {:users (db/get-users)}
          (select-keys flash [:name :email :password :errors]))))

(defn login-page [{:keys [flash] :as request}]
  (layout/render
   request
   "login.html"
   (merge {:users (db/get-users)}
          (select-keys flash [:name :email :password :errors]))))

(defn authenticate-user! [{:keys [flash] :as request}] (println request))
 
(defn index-page [request]
  (layout/render
    request
    "scores.html"
    {:scores (db/get-all-scores)}))

(defn handicap-page [request]
  (layout/render
    request
    "handicaps.html"
    {:handicaps [{:golfer_name "Clarence Bishop" :handicap 9.9}
                 {:golfer_name "Ty Elliott" :handicap 6.7}
                 {:golfer_name "Clyde Bishop" :handicap 12.8}
                 {:golfer_name "Bill Elliott" :handicap 11.4}]}))

(defn golfer-scores-page [{:keys [path-params] :as request}]
  (layout/render
    request
    "golfer_scores.html"
    {:data {:name (:golfer_name path-params) :scores (db/get-golfer-scores {:golfer_name (:golfer_name path-params)})}}))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page
         :post save-score!}]
   ["/register" {:get register-page
                 :post create-user!}]
   ["/login" {:get login-page
                 :post authenticate-user!}]
   ["/scores" {:get index-page}]
   ["/scores/:golfer_name" {:get golfer-scores-page}]
   ["/handicaps" {:get handicap-page}]])
