(ns rinha-v1.core
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [cheshire.core :as che]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]))

;; SPEC post-request
(s/def ::apelido (s/and string? #(<= (count %) 32)))
(s/def ::nome (s/and string? #(<= (count %) 100)))
(def date-regex #"^\d{4}-\d{2}-\d{2}$")
(s/def ::nascimento (s/and string? #(re-matches date-regex %)))
(s/def ::stack (s/nilable (s/coll-of (s/and string? #(<= (count %) 32)) :kind vector?)))
(s/def ::body (s/keys :req-un [::apelido ::nome ::nascimento ::stack]))

;; MOCK DB
(def db-mock (atom {}))

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn invalid-request? [body]
  (or (some #(= (:apelido body) %) (map :apelido (vals @db-mock)))
      (nil? (:apelido body))
      (nil? (:nome body))))

(defn syntactically-invalid-requests?
  [body]
  (not (s/valid? ::body body)))

(defn create-people [request]
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

(defn get-people [request]
  (let [params (:route-params request)
        id (:id params)
        result (get @db-mock id)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body result}))

(defn filter-by-term [term results]
  (che/generate-string
   (filterv
    (fn [r]
      (or (= (str/lower-case (:apelido r)) term)
          (str/includes? (str/lower-case (:nome r)) term)
          (->> (:stack r)
               (map #(str/lower-case %))
               (some #(= term %)))))
    results)))

(defn get-by-termo [request]
  (let [t (str/lower-case (get (:query-params request) "t"))
        results (vals @db-mock)]
    (if (empty? t)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body {:error "Bad Request"}}
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (filter-by-term t results)})))

(defn count-people [_]
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

(defn -main [& _args]
  (run-jetty app {:port 3000 :join? true}))
