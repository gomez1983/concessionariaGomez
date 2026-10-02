# Detecção de Stack e Versões do Projeto

Antes de auditar qualquer código ou dependência, determine a arquitetura real do repositório ou do caminho inspecionado.

---

## 1. Backend (Java / Spring Boot)

### 1.1 Localizar o arquivo de gerenciamento de dependências
- Checar presença de `pom.xml` (Maven) ou `build.gradle` / `build.gradle.kts` (Gradle).
- Ler versões exatas das propriedades:
  - Versão do Java (`<java.version>`): ex. `21`
  - Versão do Spring Boot (`<parent>` ou `<version>` em `spring-boot-starter-parent`): ex. `3.3.4`
  - Módulos ativos no pom:
    - Web: `spring-boot-starter-web`
    - Dados: `spring-boot-starter-data-jpa`
    - Validação: `spring-boot-starter-validation`
    - Migrações: `flyway-core`, `flyway-mysql`
    - Documentação: `springdoc-openapi-starter-webmvc-ui`
    - Segurança: `spring-boot-starter-security` (se presente)
    - Actuator: `spring-boot-starter-actuator` (se presente)

### 1.2 Gates de Versão Spring Boot
- **Spring Boot 3.x+:**
  - Pacote padrão de anotações: `jakarta.*` (ex.: `jakarta.persistence.*`, `jakarta.validation.*`). A presença de `javax.*` indica código residual ou dependência defasada.
  - Exception Handling: Padrão RFC 7807 (*Problem Details*) implementado nativamente ou via extensão de `ResponseEntityExceptionHandler`.
  - Tratamento de status: `HttpStatusCode` em substituição aos métodos legados de `HttpStatus`.
- **Spring Boot 2.x (Legado):**
  - Utilizava `javax.*`.

---

## 2. Frontend (React / Vite / TypeScript — se presente)

### 2.1 Localizar gerenciador e lockfile
- Inspecionar `package.json` e arquivo de lock (`package-lock.json`, `pnpm-lock.yaml`, `yarn.lock`).
- Identificar versões principais instaladas:
  - `react`, `react-dom`: v18 ou v19+
  - Bundler/Framework: `vite`, `next`, `cra`
  - Ferramentas de validação: `zod`, `yup`
  - Requisições HTTP: `axios`, `fetch` nativo

---

## 3. Modelo de Registro no Relatório

Ao iniciar o relatório, sempre inclua o bloco formatado com os dados identificados:

```markdown
### 🔍 Stack Detectado
- **Backend:** Java 21, Spring Boot 3.3.4 (Maven)
- **Persistência & Banco:** Spring Data JPA / Hibernate 6, MySQL 8.0, Flyway
- **Módulos Chave:** Bean Validation (Jakarta), SpringDoc OpenAPI 2.6.0, ModelMapper 3.2.1
- **Frontend:** [Não detectado no escopo atual / React Vite vX.X.X]
- **Segurança Declarada:** [Spring Security / Controle Customizado via Controller]
```
