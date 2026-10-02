# Changelog

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

---

## [0.1.1-BETA] - 2026-10-02

### Adicionado
- **Skill de Auditoria de Segurança Automatizada (`security-audit`):**
  - Implementação da skill adaptada em `.agents/skills/security-audit/` para análise de segurança de APIs Spring Boot e frontends React.
  - Criação de pasta padronizada para relatórios históricos em `docs/security/audits/` e documentação de apoio em `GUIA_SECURITY_AUDIT.md`.
- **Validação Temporal de Entrada (`@PastOrPresent`):**
  - Adicionada anotação `@PastOrPresent` no atributo `dataCompra` de `CarroInput`.
  - Implementada suíte de testes unitários `CarroInputTest` cobrindo rejeição de datas futuras e aceitação de datas passadas/nulas.
  - *Motivação:* Prevenir a inserção de datas de compra artificiais no futuro durante requisições de cadastro (`POST`).

### Modificado
- **Externalização Segura de Configurações de Banco de Dados:**
  - Configurações `spring.datasource.url`, `spring.datasource.username` e `spring.datasource.password` em `application.properties` alteradas para utilizar variáveis de ambiente (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) mantendo os valores de desenvolvimento local como fallback seguro.
  - *Motivação:* Permitir injeção dinâmica de credenciais em ambientes de homologação, CI/CD e produção sem expor senhas locais hardcoded.

---

## [0.1.0-BETA] - 2026-09-14

### Adicionado
- **Migração Financeira para `BigDecimal`:**
  - Substituição de `Double`/`double` por `BigDecimal` nas entidades e DTOs (`Carro`, `CarroInput`, `CarroModel`).
  - Cálculo de margem de venda com precisão decimal e arredondamento padrão bancário (`RoundingMode.HALF_UP`).
  - Migration Flyway `V004__altera-compra-para-decimal.sql` convertendo a coluna `compra` para `decimal(10, 2) not null`.
  - *Motivação:* Evitar dízimas periódicas e perda de centavos geradas pelo ponto flutuante binário do `double`, garantindo precisão e conformidade contábil/financeira nos cálculos e persistência.
- **Extração da Regra de Negócio de Venda para o Domínio:**
  - Métodos `calcularPrecoVendaSugerido()` centralizados na entidade `Carro`.
  - *Motivação:* Respeitar o princípio de Modelo Rico (*Rich Domain*) do DDD. DTOs devem apenas transportar dados, enquanto regras de negócio e cálculo de preços pertencem ao domínio, facilitando reutilização e testes.
- **Suporte a Validações no Payload:**
  - Anotação `@PositiveOrZero` no campo `compra` em `CarroInput` e `@NotBlank` em `modelo`.
  - *Motivação:* Fail-fast. Rejeitar requisições inválidas na camada de entrada antes de atingir o banco ou disparar regras de negócio.
- **Documentação Interativa com SpringDoc (OpenAPI 3 / Swagger):**
  - Adicionada dependência `springdoc-openapi-starter-webmvc-ui` (v2.6.0) em `/swagger-ui/index.html`.
  - *Motivação:* O antigo Springfox foi descontinuado e é incompatível com o Spring Boot 3; o SpringDoc é o padrão oficial de mercado para OpenAPI 3.
- **Documentação de Engenharia e Roadmap:**
  - Criação de `ROADMAP.md`, `CHANGELOG.md` e guias aprofundados na pasta `docs/` (`GUIA_CONCEITOS_E_ARQUITETURA.md` e `ENTENDENDO_O_REST_ASSURED.md`).
  - *Motivação:* Rastreabilidade de decisões, nivelamento técnico e guia de evolução do produto.

### Modificado
- **Upgrade de Stack Tecnológico (Spring Boot 3.3.4 & Java 21):**
  - Migração completa dos pacotes legados `javax.*` para `jakarta.*` (`jakarta.persistence`, `jakarta.validation`).
  - Atualização do driver MySQL para `com.mysql:mysql-connector-j` e adição do `flyway-mysql`.
  - Atualização do `ApiExceptionHandler` para o padrão do Spring 6 (`HttpStatusCode`).
  - Atualização do ModelMapper (`3.2.1`) e Lombok (`1.18.34`) para suporte nativo ao Java 21.
  - *Motivação:* Descontinuação de suporte (EOL) do ecossistema Java EE / Spring Boot 2.x, blindagem contra vulnerabilidades conhecidas e ganho de performance com o runtime Java 21.
- **Refatoração dos Testes de Integração (REST-Assured):**
  - Atualização da classe `CadastroCarroIT` para operar na porta `3307`, com banco isolado `concessionaria_test` e asserções com `BigDecimal`.
  - Cobertura expandida para 100% dos fluxos CRUD e validação de código de status (`200`, `201`, `204`, `400`, `404`).
