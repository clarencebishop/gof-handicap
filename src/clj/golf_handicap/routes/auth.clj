(ns golf-handicap.routes.auth
    (:require [golf-handicap.db.core :as db]
              [ring.util.http-response :as response]
              [bouncer.core :as b]
              [bouncer.validators :as v]
              [buddy.hashers :as hashers]
              [clojure.tools.logging :as log]))

(defn registration-errors [{:keys [password-confirm] :as params}]
  (first
    (b/validate
      params
      :name v/required
      :username v/required
      :email v/required
      :password [v/required])))

(defn handle-registration-error [e]
  (if (and
        (instance?  java.sql.SQLException e)
        (-> e (.getNextException)
            (.getMessage)
            (.startsWith "ERROR: duplicate key value")))
    (response/precondition-failed!
      {:result :error
       :message "user with the selected ID already exists"})
    (do (log/error e)
        (response/internal-server-error
          {:result :error
           :message "server error occurred while adding the user"}))))


(defn register! [{:keys [session]} user]
    (if (registration-errors user)
        (response/precondition-failed {:result :error})
        (try
            (db/create-user!
                (-> user
                    (dissoc :password-confirm)
                    (update :password hashers/encrypt)))
            (-> {:result :ok}
                (response/ok)
                (assoc :session (assoc session :identity (:id user))))
         (catch Exception e
              (handle-registration-error e)))))

(defn decode-auth [encoded]
  (let [auth (second (.split encoded " "))]
    (-> (.decode (java.util.Base64/getDecoder) auth)
        (String. (java.nio.charset.Charset/forName "UTF-8"))
        (.split ":"))))

(defn authenticate-user [[username password]]
  (log/info (str "username = " username ", password = " password))
  (when-let [user (db/get-user-by-username {:username username})]
    (when (hashers/check password (:password user))
      (:id user))))

(defn login! [{:keys [session params]}]
  (if-let [id (authenticate-user [(:username params) (:password params)])]
       (-> {:result :ok}
           (response/ok)
           (assoc :session (assoc session :identity id)))
       (response/unauthorized {:result :unauthorized
                               :message "login failure"})))

(defn logout! []
  (-> {:result :ok}
      (response/ok)
      (assoc :session nil)))

