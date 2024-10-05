(ns rinha-v1.core-test
  (:require [clojure.test :refer :all]
            [rinha-v1.core :refer :all]
            [ring.mock.request :as mock]
            [cheshire.core :as json]))

(use-fixtures :once (fn [f] (with-redefs [uuid (fn [] "23b56302-f05b-42e1-8edd-48077e78a05f")] (f))))

(defn mock-db
  []
  [{:id "f7379ae8-8f9b-4cd5-8221-51efe19e721b"
    :apelido "josé"
    :nome "José Roberto"
    :nascimento "2000-10-01"
    :stack ["C#" "Node" "Oracle"]}
  {:id "5ce4668c-4710-4cfb-ae5f-38988d6d49cb"
   :apelido "ana"
   :nome "Ana Barbosa"
   :nascimento "1985-09-23"
   :stack ["Node" "Postgres"]}])

(defn data
  [apelido nome nascimento stack]
  {:apelido apelido
   :nome nome
   :nascimento nascimento
   :stack stack})

(deftest app-test
  (testing "POST /pessoas"
    (testing "Requisição Válida"
      (let [response (app (-> (mock/request :post "/pessoas")
                              (mock/json-body (data "jose" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))]
        (is (= 201 (:status response)))
        (is (= {"Location" "/pessoas/23b56302-f05b-42e1-8edd-48077e78a05f"}
               (:headers response)))))

    (testing "Requisição Válida - stack = nil"
      (let [response (app (-> (mock/request :post "/pessoas")
                              (mock/json-body (data "jose" "José Roberto" "2000-10-01" nil))))]
        (is (= 201 (:status response)))
        (is (= {"Location" "/pessoas/23b56302-f05b-42e1-8edd-48077e78a05f"}
               (:headers response)))))

    (testing "Requisição inválida - 'jose' já foi criado em outra requisição"
      (with-redefs [db (fn [] [{:apelido "jose"}])]
        (let [response (app (-> (mock/request :post "/pessoas")
                                (mock/json-body (data "jose" "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))]
          (is (= 422 (:status response))))))

    (testing "Requisição inválida - ':apelido' não pode ser nulo"
      (let [response (app (-> (mock/request :post "/pessoas")
                              (mock/json-body (data nil "José Roberto" "2000-10-01" ["C#" "Node" "Oracle"]))))]
        (is (= 422 (:status response)))))

    (testing "Requisição inválida - ':nome' não pode ser nulo"
      (let [response (app (-> (mock/request :post "/pessoas")
                              (mock/json-body (data "jose" nil "2000-10-01" ["C#" "Node" "Oracle"]))))]
        (is (= 422 (:status response)))))

    (testing "Requisição inválida - ':nome' é obrigatório ser uma string"
      (let [response (app (-> (mock/request :post "/pessoas")
                              (mock/json-body (data "jose" 1 "2000-10-01" ["C#" "Node" "Oracle"]))))]
        (is (= 400 (:status response)))))

    (testing "Requisição inválida - ':stack' é obrigatório conter apenas strings"
      (let [response (app (-> (mock/request :post "/pessoas")
                              (mock/json-body (data "jose" 1 "2000-10-01" ["C#" 1]))))]
        (is (= 400 (:status response))))))

  #_(testing "GET /pessoas/:id"
    (let [response (app (mock/request :get "/pessoas/23b56302-f05b-42e1-8edd-48077e78a05f"))]
      (is (= 200 (:status response)))
      (is (= {:id "23b56302-f05b-42e1-8edd-48077e78a05f"
              :apelido "jose"
              :nome "José Roberto"
              :nascimento "2000-10-01"
              :stack ["C#" "Node" "Oracle"]}
             (:body response)))))

  #_(testing "GET /pessoas?t=[:termo da busca]"
    (testing "t=node"
      (let [response (app (mock/request :get "/pessoas?t=node"))]
        (is (= 200 (:status response)))
        (is (= [{:id "f7379ae8-8f9b-4cd5-8221-51efe19e721b"
                 :apelido "josé"
                 :nome "José Roberto"
                 :nascimento "2000-10-01"
                 :stack ["C#" "Node" "Oracle"]}
                {:id "5ce4668c-4710-4cfb-ae5f-38988d6d49cb"
                 :apelido "ana"
                 :nome "Ana Barbosa"
                 :nascimento "1985-09-23"
                 :stack ["Node" "Postgres"]}]
               (:body response)))))

    (testing "t=berto"
      (let [response (app (mock/request :get "/pessoas?t=node"))]
        (is (= 200 (:status response)))
        (is (= [{:id "f7379ae8-8f9b-4cd5-8221-51efe19e721b"
                 :apelido "josé"
                 :nome "José Roberto"
                 :nascimento "2000-10-01"
                 :stack ["C#" "Node" "Oracle"]}]
               (:body response)))))

    (testing "t=Python(não encontrou nada)"
      (let [response (app (mock/request :get "/pessoas?t=node"))]
        (is (= 200 (:status response)))
        (is (= []
               (:body response)))))

    (testing "t="
      (let [response (app (mock/request :get "/pessoas?t=node"))]
        (is (= 400 (:status response))))))

  #_(testing "GET /contagem-pessoas"
    (let [response (app (mock/request :get "/contagem-pessoas"))]
      (is (= 200 (:status response)))
      (is (= "2" (:body response))))))

#_(deftest valid-username?-test
  (testing "Apelido obrigaório"
    (is (= true (valid-username? "jose"))))

  (testing "Apelido único"
    (is (= true (valid-username? "jose"))))

  (testing "Apelido é uma string até 32 caracteres"
    (is (= true (valid-username? "jose"))))

  (testing "Apelido nulo"
    (is (= false (valid-username? "frodo"))))

  (testing "Apelido não é nulo"
    (is (= false (valid-username? "frodo"))))

  (testing "Apelido com uma string maior que 32 caracteres"
    (is (= false (valid-username? "pneumoultramicroscopicossilicovulcanoconiótico")))))

#_(deftest valid-name?-test
  (testing "Nome obrigatório"
    (is (= true (valid-name? "José Roberto"))))

  (testing "Nome é uma string até 100 caracteres."
    (is (= true (valid-name? "José Roberto"))))

  (testing "Nome nulo"
    (is (= false (valid-name? nil))))

  (testing "Nome inválido"
    (is (= false (valid-name? 1))))

  (testing "Nome é uma string com mais de 100 caracteres."
    (is (= true (valid-name? "pneumoultramicroscopicossilicovulcanoconióticopneumoultramicroscopicossilicovulcanoconióticopneumoult")))))

#_(deftest valid-birth-date?-test
  (testing "Nascimento obrigatório no formato de AAAA-MM-DD e string"
    (is (= true (valid-birth-date? "2000-10-01"))))

  (testing "Nascimento nulo"
    (is (= false (valid-birth-date? nil))))

  (testing "Nascimento no formato incorreto"
    (is (= false (valid-birth-date? "01-10-2000")))))

#_(deftest valid-stack?-test
  (testing "Stack nulo"
    (is (= true (valid-stack? nil))))

  (testing "Vetor de strings até 32 caracteres"
    (is (= true (valid-stack? ["C#" "Node" "Oracle"]))))

  (testing "Vetor com elemento inválido - inteiro"
    (is (= false (valid-stack? ["C#" 1]))))

  (testing "Vetor com elemento inválido - string > 32 caracteres"
    (is (= false (valid-stack? ["C#" "pneumoultramicroscopicossilicovulcanoconiótico"])))))
