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

(defn valid-body?
  [body]
  ;; consultar no db o apelido pra ver se ja existe
  (not (some #(= (:apelido %) (:apelido body)) (db))))

(defn create-people
  [request]
  (let [body (:body request)]
    (if (valid-body? body)
      {:status 201
       :headers {"Location" (str "/pessoas/" (uuid))}}
      {:status 422})))

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
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (run-jetty app {:port 3000 :join? true}))
