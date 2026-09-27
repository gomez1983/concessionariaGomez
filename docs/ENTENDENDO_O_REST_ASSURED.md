# 🤖 Entendendo o REST-Assured: Conceito, Papel e a Analogia do "Robô Invisível"

Documento de referência técnica criado em **24/09/2026** para esclarecer o papel do **REST-Assured** no ecossistema de testes da **Concessionária Gomez API**.

---

## 🎯 Em Poucas Palavras: O que é o REST-Assured?

O **REST-Assured** é uma biblioteca Java de código aberto projetada exclusivamente para **testar e validar APIs RESTful de forma 100% automatizada**.

> [!NOTE]
> **Ele tem tela ou interface gráfica?**  
> ❌ **NÃO.** O REST-Assured não possui página web, botões, nem painel visual de banco de dados.  
> Ele é **puro código Java** executado pelo compilador e pelo motor de testes (JUnit) nos bastidores.

---

## 🎭 Comparativo de Ferramentas: Quem faz o quê?

Para entender exatamente onde o REST-Assured se encaixa, compare-o com as outras ferramentas visuais do projeto:

| Ferramenta | Tem Tela / Interface Gráfica? | Quem usa e como? | Papel Principal no Projeto |
| :--- | :---: | :--- | :--- |
| **DBeaver** | ✅ **Sim (Desktop)** | O desenvolvedor abre para ver tabelas, linhas, colunas e rodar SQL. | **Gerenciador Visual do Banco de Dados** (MySQL :3307) |
| **Swagger UI** | ✅ **Sim (Navegador)** | O desenvolvedor ou frontend abre (`/swagger-ui/index.html`) e clica nos endpoints para testar requisições. | **Documentação Viva e Testes Manuais** |
| **Postman** | ✅ **Sim (Desktop/Web)** | O desenvolvedor seleciona rotas, preenche JSONs e clica no botão "Send". | **Cliente HTTP para Testes Manuais e Exploratórios** |
| **REST-Assured** | ❌ **NÃO (Puro Código Java)** | O **computador** executa automaticamente via JUnit (`./mvnw test` ou botão Run na IDE). | **Robô de Testes Automatizados de Ponta a Ponta** |

---

## 🤖 A Analogia do "Robô Invisível"

Imagine o seguinte desafio de desenvolvimento:

> *"Acabamos de migrar o projeto do Spring Boot 2 para o Spring Boot 3, mudamos todos os pacotes de `javax.*` para `jakarta.*`, atualizamos o driver do MySQL e refatoramos o cálculo da margem de lucro de 10% para dentro da entidade `Carro`.*  
> ***Como ter certeza absoluta de que NADA na API quebrou?***"

### Cenário 1: Testando Apenas com Swagger ou Postman (Manual)
Se você dependesse apenas de ferramentas visuais:
1. Você teria que abrir o navegador no Swagger UI.
2. Clicar no endpoint `GET /carros`, apertar "Execute" e olhar se retornou a lista.
3. Clicar no `POST /carros`, digitar um carro com preço de compra e conferir se gravou.
4. Clicar no `GET /carros/{id}` e conferir se o preço sugerido de venda calculou os 10% no centavo.
5. Clicar no `PUT /carros/{id}`, mandar atualizar e conferir se a data da compra não foi apagada.
6. Clicar no `DELETE /carros/{id}/ativo` para inativar e verificar o status 204.
7. Clicar no `DELETE /carros/{id}` para exclusão física e depois tentar buscar de novo para ver se dá 404.

* **Problema:** Você precisaria gastar de 10 a 20 minutos repetindo esse roteiro manual **toda vez** que mexer em qualquer linha de código. É lento, cansativo e suscetível a esquecimento humano.

---

### Cenário 2: Testando com REST-Assured (Automatizado)
Com o REST-Assured:
* Você escreve esse mesmo roteiro em código Java uma única vez (no arquivo [`CadastroCarroIT.java`](file:///C:/Projetos/concessionaria/src/test/java/com/concessionaria/gomez/CadastroCarroIT.java)).
* Você clica com o botão direito no arquivo no IntelliJ e aperta **Run** (ou digita `./mvnw test` no terminal).
* Em **3 segundos**, nos bastidores (sem abrir janela nenhuma):
  1. O robô sobe um servidor Spring Boot temporário em uma porta livre.
  2. Reseta o banco de testes (`concessionaria_test`).
  3. Dispara as 10 requisições HTTP reais.
  4. Analisa cada cabeçalho, cada status HTTP e cada campo dos JSONs retornados.
  5. Confere matematicamente os centavos da margem de 10%.
  6. Mostra uma barra verde no IntelliJ: **"9 tests passed"**. Se algo quebrar, a barra fica vermelha e aponta a linha exata do erro.

---

## 📝 Como é a Sintaxe? (O Padrão *Given / When / Then*)

O REST-Assured utiliza uma sintaxe fluente inspirada no BDD (*Behavior-Driven Development*), que se lê quase como frases em inglês:

```java
// Trecho real de CadastroCarroIT.java
given()
    .pathParam("carroId", carroCivicExistente.getId())
    .accept(ContentType.JSON)
.when()
    .get("/{carroId}")
.then()
    .statusCode(HttpStatus.OK.value())
    .body("modelo", equalTo("Civic"))
    .body("marca", equalTo("Honda"))
    .body("precoVendaSugerido", equalTo(198000.00f));
```

* **`given()` (Dado que...):** Prepara o cenário. Informa o parâmetro `carroId` e avisa que espera receber JSON.
* **`when()` (Quando...):** Dispara a ação HTTP real (`get`, `post`, `put`, `delete`).
* **`then()` (Então...):** Valida o resultado. Garante que o status foi `200 OK`, o modelo é "Civic", a marca é "Honda" e o preço de venda veio com a margem exata.

---

## 💡 Principais Benefícios para o Projeto Concessionária

1. **Prevenção de Regressões:** Permite refatorar, trocar dependências no `pom.xml` ou otimizar consultas SQL com a certeza de que nenhuma funcionalidade antiga foi corrompida.
2. **Integração Real com Banco:** Testa o ciclo completo da requisição, desde o Jackson (serialização JSON), anotações de validação (`@NotBlank`, `@PositiveOrZero`), mapeamento do Flyway até o MySQL no Docker.
3. **Ambiente Isolado:** O REST-Assured roda apontando para a base `concessionaria_test`, garantindo que os testes **nunca apaguem nem alterem** os carros cadastrados no seu banco principal de desenvolvimento (`concessionaria`).

---

## 🔗 Referências no Projeto
- Suite de Testes de Integração: [`CadastroCarroIT.java`](file:///C:/Projetos/concessionaria/src/test/java/com/concessionaria/gomez/CadastroCarroIT.java)
- Guia de Arquitetura: [`docs/GUIA_CONCEITOS_E_ARQUITETURA.md`](file:///C:/Projetos/concessionaria/docs/GUIA_CONCEITOS_E_ARQUITETURA.md)
- Roadmap de Evolução: [`ROADMAP.md`](file:///C:/Projetos/concessionaria/ROADMAP.md)
