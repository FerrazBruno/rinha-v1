(ns rinha-v1.core
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [cheshire.core :as che]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

(def db-mock (atom {}))

(defn uuid
  []
  (str (java.util.UUID/randomUUID)))

(defn invalid-request?
  [body]
  ;; consultar no db o apelido pra ver se ja existe
  (or (some #(= (:apelido body) %) (map :apelido (vals @db-mock)))
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
      (let [id (uuid)]
        (swap! db-mock assoc id (assoc body :id id))
        {:status 201
         :headers {"Location" (str "/pessoas/" id)}}))))

(defn get-people
  [request]
  (let [params (:route-params request)
        id (:id params)
        result (get @db-mock id)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body result}))

(defn get-by-termo
  [request]
  (let [t (str/lower-case (get (:query-params request) "t"))
        results (vals @db-mock)]
    (if (empty? t)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body {:error "Bad Request"}}
      (che/generate-string
       (filterv
        (fn [r]
          (or (= (str/lower-case (:apelido r)) t)
              (clojure.string/includes? (str/lower-case (:nome r)) t)
              (->> (:stack r)
                   (map #(str/lower-case %))
                   (some #(= t %)))))
        results)))))

(defn count-people
  [_]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body {:count (str (count @db-mock))}})

(defroutes app-routes
  (POST "/pessoas" [] create-people)
  (GET "/pessoas/:id" [] get-people)
  (GET "/pessoas" [] get-by-termo)
  (GET "/contagem-pessoas" [] count-people)
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
