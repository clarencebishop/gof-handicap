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
  [[:golfer st/required st/string]
   [:course st/required st/string]
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

(defn create-user! [{:keys [flash params] :as request}]
  (let [result (validate-user params) error (first result) p (second result)]
    (if (first result)
      (-> (response/found "/")
          (assoc :flash (assoc params :errors error)))
      (do
        (let [rc (auth/register! request p)]
          (log/info rc)
          (log/info p)
          (if (= (:status rc) 200)
            (layout/render request
                           "login.html"
                           (merge {:username (:username p)}
                                  (select-keys flash [:username :password :errors])))
            (layout/render request
                           "register.html"
                           (merge {:username nil} {:errors {:username (:message rc)}}
                                  (select-keys flash [:username :password :errors])))))))))

(defn home-page [request]
  (layout/render
   request
   "home.html"
   {}))

(defn login-user [{:keys [flash session] :as request}]
  (let [result (auth/login! request)]
    (log/info result)
    (if (= (:result result) :ok)
      (-> (response/found "/scores")
          (assoc :session (assoc session :identity (:id result) :username (:username result))))
      (do
        (log/info "**** LOGIN FAILED ****")
        (layout/render
         request
         "login.html"
         (merge {:username nil} {:errors {:username "username and/or password incorrect"}}
                (select-keys flash [:username :password :errors])))))))

(defn logout-user [{:keys [flash] :as request}]
  (log/info "logging out current user")
  (auth/logout!)
  ; (response/found "/home")
  (layout/render request
                 "home.html"
                 {:username nil}))

(defn enter-score [{:keys [flash session] :as request}]
  (layout/render
   request
   "enter_score.html"
   (merge {:scores (db/get-all-scores)} {:username (:username session)}
          (select-keys flash [:golfer :course :date_played :rating :slope :score :errors]))))

(defn register-page [{:keys [flash session] :as request}]
  (layout/render
   request
   "register.html"
   (merge {:users (db/get-users)} {:username (:username session)}
          (select-keys flash [:name :email :password :errors]))))

(defn login-page [{:keys [flash] :as request}]
  (layout/render
   request
   "login.html"
   (merge {:username nil}
          (select-keys flash [:username :password :errors]))))

(defn index-page [{:keys [session] :as request}]
  (layout/render
   request
   "scores.html"
   {:username (:username session)
    :scores (db/get-all-scores)}))

(defn handicap-page [{:keys [session] :as request}]
  (log/info (:session request))
  (layout/render
   request
   "handicaps.html"
   {:username (:username session)
    :handicaps [{:golfer "Clarence Bishop" :handicap 9.9}
                {:golfer "Tyrone Elliott" :handicap 6.7}
                {:golfer "Clyde Bishop" :handicap 12.8}
                {:golfer "Bill Elliott" :handicap 11.4}]}))

(defn golfer-scores-page [{:keys [params path-params session] :as request}]
  (log/info (str "***golfer-score-pagese***" params))
  (log/info (str "***golfer-score-pages***" path-params))
  (layout/render
   request
   "golfer_scores.html"
   {:username (:username session)
    :data {:name (:golfer path-params) :scores (db/get-golfer-scores {:golfer (:golfer path-params)})}}))

(defn home-routes []
  [""
   ; {:middleware [middleware/wrap-csrf
   ;             middleware/wrap-formats]}
   {:middleware [middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/score" {:get  enter-score
              :post save-score!}]
   ["/register" {:get  register-page
                 :post create-user!}]
   ["/login" {:get  login-page
              :post login-user}]
   ["/logout" {:get logout-user}]
   ["/scores" {:get index-page}]
   ["/scores/:golfer" {:get golfer-scores-page}]
   ["/handicaps" {:get handicap-page}]])
