(ns rinha-v1.core-test
  (:require [clojure.test :refer :all]
            [rinha-v1.core :refer :all]
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]))

(defn data
  [apelido nome nascimento stack]
  {:apelido apelido
   :nome nome
   :nascimento nascimento
   :stack stack})

(deftest app-test
  (testing "POST /pessoas"
    (with-redefs [uuid (fn [] "23b56302-f05b-42e1-8edd-48077e78a05f")]
      (testing "Requisição Válida"
        (let [response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))]
          (is (= 201 (:status response)))
          (is (= {"Location" "/pessoas/23b56302-f05b-42e1-8edd-48077e78a05f"}
                 (:headers response)))))

      (testing "Requisição Válida - stack = nil"
        (let [_ (reset! db-mock {})
              response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data "josé" "José Roberto" "2000-10-01" nil))))]
          (is (= 201 (:status response)))
          (is (= {"Location" "/pessoas/23b56302-f05b-42e1-8edd-48077e78a05f"}
                 (:headers response)))))

      (testing "Requisição inválida - 'josé' já foi criado em outra requisição"
        (let [_ (reset! db-mock {})
              _ (app (-> (mock/request :post "/pessoas")
                         (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))
              response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))]
          (is (= 422 (:status response)))))

      (testing "Requisição inválida - ':apelido' não pode ser nulo"
        (let [_ (reset! db-mock {})
              response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data nil "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))]
          (is (= 422 (:status response)))))

      (testing "Requisição inválida - ':nome' não pode ser nulo"
        (let [_ (reset! db-mock {})
              response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data "josé" nil "2000-10-01" ["C#" "Node" "Oracle"]))))]
          (is (= 422 (:status response)))))

      (testing "Requisição inválida - ':nome' é obrigatório ser uma string"
        (let [_ (reset! db-mock {})
              response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data "josé" 1 "2000-10-01" ["C#" "Node" "Oracle"]))))]
          (is (= 400 (:status response)))))

      (testing "Requisição inválida - ':stack' é obrigatório conter apenas strings"
        (let [_ (reset! db-mock {})
              response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data "josé" 1 "2000-10-01" ["C#" 1]))))]
          (is (= 400 (:status response)))))))

  (testing "GET /pessoas/:id"
    (with-redefs [uuid (fn [] "23b56302-f05b-42e1-8edd-48077e78a05f")]
      (let [_ (reset! db-mock {})
            _post (app (-> (mock/request :post "/pessoas")
                           (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))
            response (app (mock/request :get "/pessoas/23b56302-f05b-42e1-8edd-48077e78a05f"))]
        (is (= 200 (:status response)))
        (is (= {:id "23b56302-f05b-42e1-8edd-48077e78a05f"
                :apelido "josé"
                :nome "José Roberto"
                :nascimento "2000-10-01"
                :stack ["C#" "Node" "Oracle"]}
               (json/parse-string (:body response) true))))))

  (testing "GET /pessoas?t=[:termo da busca]"
    (testing "t=node"
      (let [_ (reset! db-mock {})
            _post1 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))
            _post2 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "ana" "Ana Barbosa" "1985-09-23" ["Node" "Postgres"]))))
            response (app (mock/request :get "/pessoas?t=node"))]
        (is (= 200 (:status response)))
        (is (= [{:apelido "josé"
                 :nome "José Roberto"
                 :nascimento "2000-10-01"
                 :stack ["C#" "Node" "Oracle"]}
                {:apelido "ana"
                 :nome "Ana Barbosa"
                 :nascimento "1985-09-23"
                 :stack ["Node" "Postgres"]}]
               (mapv #(dissoc % :id)
                     (json/parse-string (:body response) true))))))

    (testing "t=berto"
      (let [_ (reset! db-mock {})
            _post1 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))
            _post2 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "ana" "Ana Barbosa" "1985-09-23" ["Node" "Postgres"]))))
            response (app (mock/request :get "/pessoas?t=berto"))]
        (is (= 200 (:status response)))
        (is (= [{:apelido "josé"
                 :nome "José Roberto"
                 :nascimento "2000-10-01"
                 :stack ["C#" "Node" "Oracle"]}]
               (mapv #(dissoc % :id)
                     (json/parse-string (:body response) true))))))

    (testing "t=ana"
      (let [_ (reset! db-mock {})
            _post1 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))
            _post2 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "ana" "Ana Barbosa" "1985-09-23" ["Node" "Postgres"]))))
            response (app (mock/request :get "/pessoas?t=ana"))]
        (is (= 200 (:status response)))
        (is (= [{:apelido "ana"
                 :nome "Ana Barbosa"
                 :nascimento "1985-09-23"
                 :stack ["Node" "Postgres"]}]
               (mapv #(dissoc % :id)
                     (json/parse-string (:body response) true))))))

    (testing "t=Python(não encontrou nada)"
      (let [_ (reset! db-mock {})
            _post1 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))
            _post2 (app (-> (mock/request :post "/pessoas")
                            (mock/json-body (data "ana" "Ana Barbosa" "1985-09-23" ["Node" "Postgres"]))))
            response (app (mock/request :get "/pessoas?t=Python"))]
        (is (= 200 (:status response)))
        (is (= [] (json/parse-string (:body response))))))

    (testing "t="
      (let [response (app (mock/request :get "/pessoas?t="))]
        (is (= 400 (:status response)))
        (is (= "Bad Request" (-> (:body response) (json/parse-string true) (:error)))))))

  (testing "GET /contagem-pessoas"
    (let [_ (reset! db-mock {})
          _post1 (app (-> (mock/request :post "/pessoas")
                          (mock/json-body (data "josé" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))
          _post2 (app (-> (mock/request :post "/pessoas")
                          (mock/json-body (data "ana" "Ana Barbosa" "1985-09-23" ["Node" "Postgres"]))))
          response (app (mock/request :get "/contagem-pessoas"))]
      (is (= 200 (:status response)))
      (is (= "2" (-> (:body response) (json/parse-string true) (:count)))))))

(deftest spec-test
  (testing "Spec :apelido"
    (is (true? (s/valid? :rinha-v1.core/apelido "frodo")))
    (is (false? (s/valid? :rinha-v1.core/apelido nil)))
    (is (false? (s/valid? :rinha-v1.core/apelido 1)))
    (is (false? (s/valid? :rinha-v1.core/apelido "pneumoultramicroscopicossilicovulcanoconiótico"))))

  (testing "Spec :nome"
    (is (true? (s/valid? :rinha-v1.core/nome "Frodo Bolseiro")))
    (is (false? (s/valid? :rinha-v1.core/nome nil)))
    (is (false? (s/valid? :rinha-v1.core/nome "pneumoultramicroscopicossilicovulcanoconióticopneumoultramicroscopicossilicovulcanoconióticopneumoult"))))

  (testing "Spec :nascimento"
    (is (true? (s/valid? :rinha-v1.core/nascimento "1987-09-13")))
    (is (false? (s/valid? :rinha-v1.core/nascimento nil)))
    (is (false? (s/valid? :rinha-v1.core/nascimento 1)))
    (is (false? (s/valid? :rinha-v1.core/nascimento "09-13-1987")))
    (is (false? (s/valid? :rinha-v1.core/nascimento "1987-09-DD")))
    (is (false? (s/valid? :rinha-v1.core/nascimento "1987-MM-13")))
    (is (false? (s/valid? :rinha-v1.core/nascimento "AAAA-09-13")))
    (is (false? (s/valid? :rinha-v1.core/nascimento "19879-09-13")))
    (is (false? (s/valid? :rinha-v1.core/nascimento "1987-091-13")))
    (is (false? (s/valid? :rinha-v1.core/nascimento "1987-09-134"))))

  (testing "Spec :stack"
    (is (true? (s/valid? :rinha-v1.core/stack ["Clojure" "ClojureScript"])))
    (is (true? (s/valid? :rinha-v1.core/stack nil)))
    (is (false? (s/valid? :rinha-v1.core/stack ["Clojure" "pneumoultramicroscopicossilicovulcanoconiótico"])))
    (is (false? (s/valid? :rinha-v1.core/stack ["Clojure" 123])))
    (is (false? (s/valid? :rinha-v1.core/stack #{"Clojure" "ClojureScript"})))
    (is (false? (s/valid? :rinha-v1.core/stack "Clojure")))
    (is (false? (s/valid? :rinha-v1.core/stack 1234))))

  (testing "Spec body"
    (is (true? (s/valid? :rinha-v1.core/body
                         {:apelido "neo"
                          :nome "Thomas A. Anderson"
                          :nascimento "1962-03-11"
                          :stack ["c#" "javascript" "MySQL"]})))
    (is (false? (s/valid? :rinha-v1.core/body
                          {:apelido nil
                           :nome "Thomas A. Anderson"
                           :nascimento "1962-03-11"
                           :stack ["c#" "javascript" "MySQL"]})))
    (is (false? (s/valid? :rinha-v1.core/body
                          {:apelido "neo"
                           :nome nil
                           :nascimento "1962-03-11"
                           :stack ["c#" "javascript" "MySQL"]})))
    (is (false? (s/valid? :rinha-v1.core/body
                          {:apelido "neo"
                           :nome "Thomas A. Anderson"
                           :nascimento nil
                           :stack ["c#" "javascript" "MySQL"]})))
    (is (false? (s/valid? :rinha-v1.core/body
                          {:apelido "neo"
                           :nome "Thomas A. Anderson"
                           :nascimento "1962-03-11"
                           :stack ["c#" "javascript" 4321]})))))
