(ns rinha-v1.core-test
  (:require [clojure.test :refer :all]
            [rinha-v1.core :refer :all]
            [ring.mock.request :as mock]
            [cheshire.core :as json]))

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

#_(deftest valid-username?-test
  (testing "Apelido obrigaório"
    (is (= true (valid-username? "josé"))))

  (testing "Apelido único"
    (is (= true (valid-username? "josé"))))

  (testing "Apelido é uma string até 32 caracteres"
    (is (= true (valid-username? "josé"))))

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
