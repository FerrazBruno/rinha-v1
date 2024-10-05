(ns rinha-v1.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]])
  (:gen-class))

(defn uuid
  []
  (java.util.UUID/randomUUID))

(defn db
  []
  (let [db-mock (atom [])]
    @db-mock))

(defn invalid-request?
  [body]
  ;; consultar no db o apelido pra ver se ja existe
  (or (some #(= (:apelido %) (:apelido body)) (db))
      (nil? (:apelido body))
      (nil? (:nome body))))

(defn syntactically-invalid-requests?
  [body]
  (or (not (string? (:nome body)))
      (some #(not (string? %)) (:stack body))))

(defn create-people
  [request]
  (let [body (:body request)]
    (cond
      (invalid-request? body)
      {:status 422}
      (syntactically-invalid-requests? body)
      {:status 400}
      :else
      {:status 201
       :headers {"Location" (str "/pessoas/" (uuid))}})))

(defroutes app-routes
  (POST "/pessoas" [] create-people)
  (route/not-found {:status 404
                    :headers {"Content-Type" "application/json"}
                    :body {:error "Not Found"}}))

(def app
  (-> app-routes
      (wrap-params)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)))

(defn -main
  [& args]
  (run-jetty app {:port 3000 :join? true}))
