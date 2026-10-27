# 🚀 Roadmap de Evolução e Melhorias — Concessionária Gomez API

Documento criado em **13/09/2026** consolidando o diagnóstico atual da aplicação e o plano de evolução técnica e de negócios para guiar as próximas etapas de desenvolvimento.

---

## 📌 Status Atual do Projeto (Baseline)
- [x] **Ambiente Local:** Java 21 + MySQL 8.0 rodando isolado via Docker (`docker-compose.yml` na porta `3307`).
- [x] **Gerenciador de Banco:** DBeaver Community conectado e operacional (porta `3307`).
- [x] **Framework:** Spring Boot 3.3.4 com Maven e Java 21 nativo.
- [x] **Documentação da API:** OpenAPI 3 / Swagger UI moderno via SpringDoc em `/swagger-ui/index.html`.
- [x] **Banco & Migrations:** Flyway gerenciando scripts com carga de testes automática (`afterMigrate.sql`).
- [x] **Testes de API:** Coleção Postman validada (`concessionaria-postman-collection.json`) com fluxo CRUD completo de Carros, Clientes, Vendas e Fotos, paginação, ordenação, Specifications, HATEOAS e suite de 71 testes unitários + testes integrados.
- [x] **Auditoria de Segurança:** Skill automatizada integrada (`.agents/skills/security-audit/`).

---

## 🗺️ Fases Sugeridas de Evolução

```mermaid
graph TD
    A[Fase 1: Qualidade e Precisão Financeira] --> B[Fase 2: Modernização do Stack Spring Boot 3]
    B --> C[Fase 3: Recursos Avançados de API]
    C --> D[Fase 4: Expansão do Domínio de Negócio]
    D --> E[Fase 5: Frontend Moderno SPA]
    E --> F[Fase 6: DevOps e Containerização Total]
    F --> G[Fase 7: Propostas Comerciais e Negociação Assistida]
```

---

### 🔹 Fase 1: Qualidade de Código & Precisão Financeira (Quick Wins)
- [x] **Migração de `Double`/`double` para `BigDecimal`:**
  - Substituir tipos primitivos de ponto flutuante binário por `BigDecimal` nos campos de valor (`compra` e `venda`).
  - Evitar dízimas e imprecisões contábeis no arredondamento financeiro.
  - Migration Flyway `V004__altera-compra-para-decimal.sql` adicionada.
- [x] **Extração da Regra de Negócio de Venda:**
  - Mover o cálculo da margem de 10% de `CarroModel.java` (camada de apresentação/DTO) para o domínio (`Carro.java` ou `CalculadoraMargemService`).
  - Permitir parametrização futura da margem de lucro por veículo ou categoria.
- [x] **Revisão e Expansão dos Testes de Integração:**
  - Testes unitários (`CarroTest`, `CarroModelAssemblerTest`) validados.
  - Suite de testes de integração com REST-Assured em `CadastroCarroIT.java` expandida para cobrir 100% dos fluxos CRUD e regras de negócio:
    - `GET /carros` (listagem completa e contagem)
    - `GET /carros?ano={ano}` (filtro por ano)
    - `GET /carros/{id}` (sucesso e 404 para ID inexistente)
    - Validação de cálculo da margem sugerida de 10% no endpoint de busca
    - `POST /carros` (cadastro com sucesso 201 e validação de schema 400 Bad Request)
    - `PUT /carros/{id}` (atualização completa 200)
    - `PUT /carros/{id}/ativo` (ativação 204 No Content)
    - `DELETE /carros/{id}/ativo` (inativação 204 No Content)
    - `DELETE /carros/{id}` (exclusão física 204 No Content e validação 404)

---

### 🔹 Fase 2: Modernização do Stack Tecnológico & Blindagem de Atualizações
- [x] **Upgrade para Spring Boot 3.3.4 e Java 21:**
  - Migradas todas as dependências e anotações do ecossistema legado `javax.*` para `jakarta.*` (`jakarta.persistence`, `jakarta.validation`).
  - Atualizado `ApiExceptionHandler` para suportar `HttpStatusCode` do Spring 6.
  - Driver MySQL modernizado para `com.mysql:mysql-connector-j` e módulo `flyway-mysql` adicionado.
- [x] **Substituição do Springfox por SpringDoc (OpenAPI 3 / Swagger Moderno):**
  - Removido `springfox-swagger2` e `springfox-swagger-ui` (descontinuados).
  - Adicionado `springdoc-openapi-starter-webmvc-ui` (v2.6.0) para documentação viva e interativa em `/swagger-ui/index.html`.
  - Migradas anotações nos controllers (`@Tag`, `@Operation`) e configuração simplificada via bean `OpenAPI`.
- [x] **Atualização das dependências auxiliares e correções estruturais:**
  - `modelmapper` atualizado para versão `3.2.1` compatível com Java 21.
  - `lombok` atualizado para `1.18.34`.
  - Correção na atualização de veículos (`PUT`): implementado `copyToDomainObject` no `CarroInputDisassembler` com `setSkipNullEnabled(true)`, removido `@PreUpdate` indevido e blindado `dataCompra` contra mutações acidentais.
  - Teste unitário `CarroInputDisassemblerTest` implementado para garantir integridade das mesclagens.
- [x] **Skill de Auditoria de Segurança Automatizada (`security-audit`):**
  - Implementada em [`.agents/skills/security-audit/`](file:///C:/Projetos/concessionaria/.agents/skills/security-audit/README.md) adaptando as melhores práticas do framework Dev Fraga para Spring Boot 3, Java 21, JPA e Frontend React (Fase 5).
  - Inclui checklists de Spring Data JPA, Mass Assignment em DTOs, rastreamento de segredos, filtros anti-falso-positivo e geração de relatórios com patches sugeridos.

---

### 🔹 Fase 3: Recursos Avançados de API RESTful (100% Concluída)
- [x] **Filtros por Ano e Faixa de Valores (`minValor`, `maxValor` e BETWEEN):**
  - **O que foi feito:** Suporte a filtros numéricos combinados de ano e preço.
  - **Como funciona:** JPQL parametrizada flexível e 100% imune a SQL Injection.
  - **Validação e Testes:** Testes integrados com REST-Assured e requisições Postman.
- [x] **Paginação e Ordenação:**
  - **O que foi feito:** Endpoint `GET /carros` atualizado para receber `Pageable` com `@PageableDefault(size = 10)`.
  - **Como funciona:** Execução via `LIMIT` e `OFFSET` com `countQuery` dedicada, devolvendo metadados de total de registros e páginas.
  - **Validação e Testes:** Teste unitário em `CarroControllerTest`, testes em `CadastroCarroIT` e Postman.
- [x] **Filtros Dinâmicos Avançados com Specification (marca, modelo, ativo):**
  - **O que foi feito:** Implementado `JpaSpecificationExecutor<Carro>`, classe de especificação `CarroSpecs` e DTO `CarroFilter`.
  - **Como funciona:**
    - Consultas dinâmicas via *Criteria API*: cláusulas `WHERE` são geradas sob demanda apenas para os parâmetros preenchidos na URL.
    - Suporte a busca textual parcial e case-insensitive para `marca` (ex.: `?marca=merc`) e `modelo` (ex.: `?modelo=classe`).
    - Filtro booleano por status ativo (`?ativo=true` ou `false`).
    - Totalmente integrado à paginação e ordenação em uma única chamada.
  - **Validação e Testes:**
    - Teste unitário `CarroSpecsTest.java` com Mockito testando predicados dinâmicos e casos nulos.
    - Testes de integração em `CadastroCarroIT.java` e novas requisições 1.8 e 1.9 no Postman.
- [x] **Suporte a Quilometragem e Filtro por Range de KM (`minQuilometragem`, `maxQuilometragem` e regra `zeroKm`):**
  - **O que foi feito:** Adicionada a coluna `quilometragem` no banco via migration `V005__adiciona-quilometragem-em-carro.sql`, campo na entidade, DTOs de entrada e saída, regra de negócio `isZeroKm()` e filtros de range no `CarroSpecs`.
  - **Como funciona:**
    - Validação de entrada `@PositiveOrZero` garantindo que não existam valores negativos.
    - Regra de negócio na entidade para identificação automática de veículos 0 km.
    - Consultas com range de km (ex.: `?minQuilometragem=10000&maxQuilometragem=50000`).
  - **Validação e Testes:**
    - Testes unitários em `CarroTest`, `CarroInputTest`, `CarroInputDisassemblerTest` e `CarroSpecsTest`.
    - Teste de integração em `CadastroCarroIT` e requisição 1.10 no Postman.
- [x] **Maturidade REST com HATEOAS (Richardson Nível 3):**
  - **O que foi feito:** Adicionada a dependência `spring-boot-starter-hateoas` e implementado modelo hipermídia com `RepresentationModel`.
  - **Como funciona:**
    - O modelo `CarroModel` passa a carregar o nó `_links` guiando o cliente da API pelas ações disponíveis:
      - `self`: link direto para o próprio recurso (`GET /carros/{id}`);
      - `foto`: link direto para os metadados da foto do veículo (`GET /carros/{id}/foto`);
      - `carros`: link para voltar à coleção completa (`GET /carros`);
      - `inativar` / `ativar`: link dinâmico condicional que informa a ação de negócio válida para aquele carro específico de acordo com seu status atual.
  - **Validação e Testes:**
    - Teste unitário `CarroModelAssemblerTest.java` validando a geração dos links `self`, `foto`, `carros`, `inativar` e `ativar`.
    - Testes integrados em `CadastroCarroIT.java` verificando a presença de `_links`.

---

### 🔹 Fase 4: Expansão do Domínio de Negócio (100% Concluída)
- [x] **Módulo de Clientes:**
  - **O que foi feito:**
    - Migration Flyway `V006__cria-tabela-cliente.sql` com constraints e unicidade de CPF/CNPJ.
    - Carga inicial em `afterMigrate.sql` com clientes PF e PJ.
    - Entidade [`Cliente`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/Cliente.java) e Value Object [`Endereco`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/Endereco.java) embutido (`@Embedded`).
    - [`ClienteRepository`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/repository/ClienteRepository.java) com `JpaSpecificationExecutor` e validação de duplicidade por CPF/CNPJ.
    - [`CadastroClienteService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/CadastroClienteService.java) com regras de negócio, ativação/inativação e exclusão protegida.
    - Exceção de domínio [`ClienteNaoEncontradoException`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/exception/ClienteNaoEncontradoException.java).
    - DTOs de entrada e saída: [`ClienteInput`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/input/ClienteInput.java), [`EnderecoInput`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/input/EnderecoInput.java), [`ClienteModel`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/ClienteModel.java) e [`EnderecoModel`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/EnderecoModel.java).
    - [`ClienteModelAssembler`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/assembler/ClienteModelAssembler.java) com suporte completo a HATEOAS (`self`, `clientes`, `ativar`, `inativar`).
    - [`ClienteInputDisassembler`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/assembler/ClienteInputDisassembler.java) blindado para cópia de dados.
    - [`ClienteController`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/controller/ClienteController.java) com CRUD completo, paginação, ordenação e documentação OpenAPI 3.
- [x] **Módulo de Propostas / Vendas:**
  - **O que foi feito:**
    - Migration Flyway `V007__cria-tabela-venda.sql` associando `Carro` e `Cliente` com chaves estrangeiras.
    - Entidade [`Venda`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/Venda.java) com data da venda automática via `@CreationTimestamp`.
    - Repositório [`VendaRepository`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/repository/VendaRepository.java) com busca otimizada (`join fetch`).
    - Serviço de negócio [`EmissaoVendaService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/EmissaoVendaService.java) com regras críticas:
      - Impede venda de veículos inativos ou já vendidos (`NegocioException`).
      - Impede venda para clientes inativos.
      - **Inativação automática do veículo no estoque** ao concretizar a venda.
      - Cancelamento de venda com reativação automática do veículo no estoque.
      - **Edição de negociação concluída (`PUT /vendas/{id}`):** permite alterar valor negociado, observações, cliente e veículo (com reativação automática do carro anterior e inativação do novo).
    - DTOs [`VendaInput`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/input/VendaInput.java), [`VendaModel`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/VendaModel.java), [`CarroResumoModel`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/CarroResumoModel.java) e [`ClienteResumoModel`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/ClienteResumoModel.java).
    - [`VendaModelAssembler`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/assembler/VendaModelAssembler.java) com links HATEOAS (`self`, `vendas`, `carro`, `cliente`, `editar`, `cancelar`).
    - [`VendaController`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/controller/VendaController.java) com listagem paginada, busca individual, emissão, edição (`PUT /vendas/{id}`) e cancelamento.
- [x] **Upload e Armazenamento de Fotos do Veículo:**
  - **O que foi feito:**
    - Migration Flyway `V008__cria-tabela-foto-carro.sql` vinculada à chave primária do carro.
    - Entidade [`FotoCarro`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/FotoCarro.java) com `@MapsId` e metadados (`nomeArquivo`, `contentType`, `tamanho`, `descricao`).
    - Interface [`FotoStorageService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/FotoStorageService.java) e implementação local [`DiscoLocalFotoStorageService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/infrastructure/service/storage/DiscoLocalFotoStorageService.java) desacoplada e preparada para substituição por S3/MinIO.
    - Configuração de storage via [`StorageProperties`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/core/storage/StorageProperties.java).
    - Serviço [`CatalogoFotoCarroService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/CatalogoFotoCarroService.java) que gerencia substituição e remoção de arquivos físicos e registros no banco com garantia transacional.
    - DTOs [`FotoCarroInput`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/input/FotoCarroInput.java) e [`FotoCarroModel`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/FotoCarroModel.java).
    - Controller [`CarroFotoController`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/controller/CarroFotoController.java) com suporte a:
      - `PUT /carros/{id}/foto` (upload multipart/form-data)
      - `GET /carros/{id}/foto` (metadados JSON com HATEOAS)
      - `GET /carros/{id}/foto` (serviço direto do binário da imagem JPEG/PNG com `InputStreamResource`)
      - `DELETE /carros/{id}/foto` (remoção da foto e do arquivo no disco)
- [x] **Qualidade & Testes Automatizados:**
  - Suite de testes expandida para **46 testes unitários e de integração com 100% de sucesso**.
  - Testes cobrindo validação de DTOs, cálculo financeiro, emissão de vendas, regras de inativação, upload/substituição de fotos e HATEOAS.
  - Coleção Postman atualizada com todas as pastas (`Carros`, `Fotos`, `Clientes`, `Vendas`).

---

### 🔹 Fase 5: Frontend Moderno (Painel do Funcionário)
- [x] **Interface implementada em código:**
  - Painel local e editável em `frontend/`, com instruções em `frontend/README.md`.
- [x] **Tecnologia: React (Vite + TypeScript) e Tailwind CSS:**
  - Interface SPA (Single Page Application) limpa, responsiva e com visual profissional.
- [x] **Dashboard Principal:**
  - Total de veículos em estoque, valor total investido e estimativa de faturamento com margem.
  - Indicadores de todo o estoque via `GET /dashboard`; preço sugerido calculado pela entidade `Carro` no backend.
- [x] **Grid / Catálogo Interativo com Cards:**
  - Exibição visual de cada carro (marca, modelo, ano, crachá "Ativo/Inativo", valor sugerido de venda).
  - Ações rápidas: botão de alternar ativo/inativo, editar e excluir com modal de confirmação.
- [x] **Formulário de Cadastro/Edição:**
  - Validação de campos em tempo real com mensagens amigáveis em português (consumindo o *Problem Details* do backend).
- [x] **Integração real de Estoque, Clientes e Vendas:**
  - CRUD, paginação e ordenação; filtros de veículos enviados à API.
  - Emissão e cancelamento de vendas com atualização do estoque pelo backend.
  - Fonte de dados de demonstração e persistência em localStorage removidas.
- [x] **Galeria com múltiplas fotos por veículo:**
  - Janela com upload múltiplo, prévias, descrição por ângulo, miniaturas, navegação, substituição e remoção com confirmação.
  - Contrato da foto principal em `/carros/{id}/foto` preservado; fotos complementares em `/carros/{id}/fotos`.
  - Migration `V009__cria-galeria-fotos-carro.sql`; JPEG/PNG até 10 MB, com validação do conteúdo.
  - Metadados no MySQL e arquivos no storage local da API (`catalogo-fotos/`).
  - Carga de exemplos `afterMigrate.sql` isolada em recursos de testes para preservar os dados reais.

---

### Ajustes de experiência do painel
- [x] Layout compacto inspirado em `concessionaria-dashboard-ui.png`, com destaque ciano e logotipos das fabricantes.
- [x] Modos escuro e claro com botão discreto no topo e preferência persistida.
- [x] Máscaras monetárias brasileiras em compra, venda e filtros; CPF/CNPJ, telefone e CEP durante a digitação.
- [x] Consulta automática ao ViaCEP, preenchimento do endereço e alternativa manual em falhas ou dados incompletos.
- [x] Fotos encapsuladas nos cards e visualização proporcional na galeria.

---
### 🔹 Fase 6: DevOps & Containerização Completa (100% Concluída)
- [x] **Dockerfile Multi-Stage para o Spring Boot:**
  - Criação do [`Dockerfile`](file:///C:/Projetos/concessionaria/Dockerfile) otimizado em dois estágios (`maven:3.9.9-eclipse-temurin-21-alpine` para build com cache de dependências e `eclipse-temurin:21-jre-alpine` para execução leve).
  - Execução segura com usuário não-root (`appuser`), diretório persistente `/app/catalogo-fotos` com permissões dedicadas e flags de JVM otimizadas para containers (`-XX:+UseG1GC -XX:MaxRAMPercentage=75.0`).
  - Arquivo [`.dockerignore`](file:///C:/Projetos/concessionaria/.dockerignore) configurado para isolar o contexto de build (excluindo `target/`, `frontend/`, `catalogo-fotos/`, `graphify-out/`, `docs/` e metadados de agentes/IDEs).
- [x] **Docker Compose Unificado:**
  - Orquestração completa em [`docker-compose.yml`](file:///C:/Projetos/concessionaria/docker-compose.yml) subindo MySQL 8.0 (`3307:3306`), API Spring Boot (`8080:8080`) e Frontend React/Nginx (`80:80`) com `docker compose up -d`.
  - Healthcheck automático no MySQL garantindo inicialização ordenada (`condition: service_healthy`) antes da subida do backend e persistência de dados via volume `mysql_data` e bind mount `./catalogo-fotos:/app/catalogo-fotos`.
  - Containerização do frontend em [`frontend/Dockerfile`](file:///C:/Projetos/concessionaria/frontend/Dockerfile) (Node 22 Alpine + Nginx Alpine) e proxy reverso em [`frontend/nginx.conf`](file:///C:/Projetos/concessionaria/frontend/nginx.conf) roteando `/api/`, `/swagger-ui/` e `/v3/api-docs` diretamente para `backend:8080` com suporte a upload de até 15 MB.
- [x] **CI/CD Básico (GitHub Actions):**
  - Pipeline automatizado em [`.github/workflows/ci.yml`](file:///C:/Projetos/concessionaria/.github/workflows/ci.yml) acionado em `push` e `pull_request` para `main`/`master`.
  - Job **Backend (Java 21 & Spring Boot):** provisiona container de serviço `mysql:8.0` com healthcheck TCP, configura JDK 21 (Temurin) com cache Maven e executa `mvn clean verify -B` (testes unitários e integrados).
  - Job **Frontend (React, Vite & Vitest):** configura Node.js 22 com cache npm e valida `npm ci`, `npm test` (Vitest) e `npm run build` (TypeScript + Vite).

---

### 🔹 Fase 7: Propostas Comerciais e Negociação Assistida (100% Concluída)
> 📄 **Especificação Técnica Completa:** [`docs/FASE_7_PROPOSTAS_E_NEGOCIACAO.md`](file:///C:/Projetos/concessionaria/docs/FASE_7_PROPOSTAS_E_NEGOCIACAO.md)

- [x] **Etapa 7.0 e 7.2 — Política Comercial e Motor Financeiro:**
  - Política versionada por contexto comercial (`LOJA_PADRAO` e `EXCECAO_VEICULO`) em [`PoliticaComercial`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/PoliticaComercial.java) com vigência, responsável, validação de invariantes (`margemDesejada >= margemAutonomia >= margemPisoAbsoluto`) e precedência explícita.
  - Motor financeiro comercial em [`MotorFinanceiroComercialService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/MotorFinanceiroComercialService.java) implementando cálculo de contribuição estimada (`P - C - F - (t * P) + B`), piso matemático (`(C + F - B) / (1 - t - m)`) arredondado para cima (`RoundingMode.CEILING`), validação de denominador (`1 - t - m > 0`), aplicação da regra mais restritiva de desconto e detecção de custos pendentes.
  - Suíte de testes unitários em [`MotorFinanceiroComercialServiceTest`](file:///C:/Projetos/concessionaria/src/test/java/com/concessionaria/gomez/domain/service/MotorFinanceiroComercialServiceTest.java) validando todos os cenários de homologação, precisão interna de centavos e regras de alçada.
- [x] **Etapa 7.1 — Modelo de Dados e Migrations Incrementais:**
  - Migration Flyway [`V010__cria-estrutura-propostas-e-politica-comercial.sql`](file:///C:/Projetos/concessionaria/src/main/resources/db/migration/V010__cria-estrutura-propostas-e-politica-comercial.sql) com FKs, índices e constraints de unicidade (`proposta_id + sequencia` e `revisao_convertida_id`).
  - Entidades de domínio implementadas: [`PropostaComercial`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/PropostaComercial.java), [`RevisaoProposta`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/RevisaoProposta.java) (imutável após submissão), [`ItemFinanceiroRevisao`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/ItemFinanceiroRevisao.java), [`CustoVeiculo`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/CustoVeiculo.java), [`PagamentoProposta`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/PagamentoProposta.java), [`FinanciamentoProposta`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/FinanciamentoProposta.java), [`AvaliacaoTroca`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/AvaliacaoTroca.java), [`CampanhaComercial`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/CampanhaComercial.java) e [`DecisaoAprovacao`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/DecisaoAprovacao.java).
  - Separação entre [`DisponibilidadeComercial`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/DisponibilidadeComercial.java) (`DISPONIVEL`, `RESERVADO`, `VENDIDO`) e ativação cadastral em [`Carro`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/Carro.java), além de vínculo opcional `revisaoConvertida` e flag `origemLegada` em [`Venda`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/Venda.java).
- [x] **Etapa 7.3 — Ciclo de Vida da Proposta, Alçadas de Aprovação e API:**
  - Orquestração completa em [`GestaoPropostaComercialService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/GestaoPropostaComercialService.java) com máquina de estados (`RASCUNHO -> EM_NEGOCIACAO -> PENDENTE_APROVACAO -> APROVADA -> CONVERTIDA`), liberação automática dentro da autonomia, fila gerencial com bloqueio de autoaprovação (`403`) e verificação de concorrência de revisão (`409`).
  - Endpoints RESTful com OpenAPI e HATEOAS em [`PropostaComercialController`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/controller/PropostaComercialController.java) (`/propostas`, `/propostas/simulacoes`, `/propostas/{id}/revisoes`, `/propostas/{id}/aprovacoes`, `/propostas/{id}/reservas`, `/propostas/{id}/converter-venda`, `/propostas/politicas`) e ocultação de custos/margens internas no JSON para o perfil `VENDEDOR`.
- [x] **Etapa 7.4 — Frontend (Atendimento, Simulação e Fila Gerencial):**
  - Menu **Propostas** em [`Proposals.tsx`](file:///C:/Projetos/concessionaria/frontend/src/components/Proposals.tsx) e integração em [`App.tsx`](file:///C:/Projetos/concessionaria/frontend/src/App.tsx) com seletor de perfil operacional (`VENDEDOR`, `GERENTE`, `ADMIN`), listagem paginada, filtros de situação e simulação em tempo real com debounce e `AbortController`.
  - Indicadores visuais e textuais de alçada, fila gerencial de decisões e modal de resumo comercial para o cliente sem exposição de custos ou margens internas.
- [x] **Etapa 7.5 — Usado na Troca, Financiamento e Campanhas:**
  - Avaliação de veículo usado separando valor técnico, valor comercial aceito, saldo de quitação verificado e crédito líquido na troca, contabilizando eventual superavaliação suportada pela loja como concessão econômica na revisão e incorporando o usado ao estoque apenas no fechamento efetivo.
  - Suporte a composição de pagamentos reconciliada, financiamento (principal, entrada, prazo, taxa, CET) e campanhas comerciais parametrizadas.
- [x] **Etapas 7.6 e 7.7 — Fechamento de Vendas, Indicadores e Homologação:**
  - Conversão idempotente e transacional de revisão autorizada em [`Venda`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/model/Venda.java) com valores congelados, integração com `POST /vendas` e cancelamento auditado (`POST /vendas/{id}/cancelamento-auditado`) em [`EmissaoVendaService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/EmissaoVendaService.java).
  - Evolução dos indicadores em [`ResumoEstoqueService`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/domain/service/ResumoEstoqueService.java) e [`DashboardModel`](file:///C:/Projetos/concessionaria/src/main/java/com/concessionaria/gomez/api/model/DashboardModel.java), além de suíte de testes unitários em [`GestaoPropostaComercialServiceTest`](file:///C:/Projetos/concessionaria/src/test/java/com/concessionaria/gomez/domain/service/GestaoPropostaComercialServiceTest.java).

---

## 🛠️ Como Consultar ou Executar
Quando for iniciar uma etapa, basta abrir este arquivo no projeto (`ROADMAP.md`), escolher a tarefa e iniciamos com commits pontuais e testes a cada passo!
