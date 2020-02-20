(ns golf-handicap.routes.home
  (:require
   [golf-handicap.layout :as layout]
   [golf-handicap.db.core :as db]
   [clojure.java.io :as io]
   [golf-handicap.middleware :as middleware]
   [golf-handicap.routes.auth :as auth]
   [clojure.tools.logging :as log]
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

(defn create-user! [{:keys [params] :as request}]
  (let [result (validate-user params) error (first result) p (second result)]
       (if (first result)
         (-> (response/found "/")
             (assoc :flash (assoc params :errors error)))
         (do
           (auth/register! request p)
           (response/found "/")))))

(defn my-login-user [request]
  (if (auth/login! request)
    (response/found "/scores")
    (response/found "/login")))

(defn login-user [{:keys [params] :as request}]
  (let [result (auth/login! request)]
    (log/info (str "result - " result))
    (if (= (get-in result [:body :result]) :ok)
      (response/found "/scores")
      (do
        (log/info "**** LOGIN FAILED ****")
        (-> (response/found "/login")
            (assoc :flash (assoc params :errors (get-in result [:body :message]))))))))

(defn logout-user [request]
  (log/info "logging out current user")
  (auth/logout!)
  (response/found "/login"))

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
   ; {:middleware [middleware/wrap-csrf
   ;             middleware/wrap-formats]}
   {:middleware [middleware/wrap-formats]}
   ["/" {:get  home-page
         :post save-score!}]
   ["/register" {:get  register-page
                 :post create-user!}]
   ["/login" {:get  login-page
              :post login-user}]
   ["/logout" {:get logout-user}]
   ["/scores" {:get index-page}]
   ["/scores/:golfer_name" {:get golfer-scores-page}]
   ["/handicaps" {:get handicap-page}]])
