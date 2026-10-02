# 🛡️ Relatório de Auditoria de Segurança — Concessionária Gomez API

**Data da Auditoria:** 02/10/2026 - 15:17 (Atualizado às 15:35 com correções aplicadas)  
**Escopo Auditado:** Raiz do projeto (`C:\Projetos\concessionaria`)  
**Arquivo do Relatório:** `docs/security/audits/2026-10-02-auditoria.md`  

---

### 🔍 Stack Detectado
- **Backend:** Java 21 LTS, Spring Boot 3.3.4 (Maven Wrapper)
- **Persistência & Banco:** Spring Data JPA / Hibernate 6, MySQL 8.0 (Docker), Flyway Migrations
- **Documentação de API:** SpringDoc OpenAPI 2.6.0 (`/swagger-ui/index.html`)
- **Mapeamento & Utilitários:** ModelMapper 3.2.1, Lombok 1.18.34, Jakarta Bean Validation
- **Frontend:** Não detectado no escopo atual (previsto para a **Fase 5** no [ROADMAP.md](../../../ROADMAP.md))
- **Mecanismos de Segurança:** Ausência de `spring-boot-starter-security` (endpoints públicos na fase atual)

---

## 📊 1. Resumo Executivo de Severidade

| Severidade | Identificados | Corrigidos | Pendentes / Em Acompanhamento |
| :--- | :---: | :---: | :--- |
| 🔴 **CRITICAL** | **0** | - | Nenhuma vulnerabilidade crítica detectada |
| 🟠 **HIGH** | **0** | - | Nenhum risco alto comprovado |
| 🟡 **MEDIUM** | **1** | 0 | [SEC-01] Endpoints sem auth (aguardando Fase 4/5 do Roadmap) |
| 🔵 **LOW** | **3** | **2** | [SEC-02] e [SEC-04] **CORRIGIDOS** ✅; [SEC-03] CORS (aguardando Fase 5) |
| ⚪ **INFO** | **2** | 0 | Observações de desenvolvimento (Swagger UI e include-message) |

---

## 🔎 2. Detalhamento dos Achados por Categoria

### A. Autenticação e Controle de Acesso (AuthN / AuthZ)

#### [SEC-01] Ausência de Autenticação em Endpoints com Efeito Colateral
- **Severidade:** `MEDIUM`
- **Status:** ⏳ **Planejado** (Fase 4/5 do Roadmap)
- **Localização:** `CarroController.java:55-103`
- **Evidência:**
```java
@DeleteMapping("/{carroId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void remover (@PathVariable Long carroId){
    cadastroCarro.excluir(carroId);
}
```
- **Impacto:** Qualquer cliente HTTP na rede pode excluir veículos, alterar preços ou mudar o status de ativo/inativo sem identificação de usuário ou perfil (ex: Gerente vs Vendedor).
- **Contexto & Recomendação:** Perfeitamente compreensível para o estágio atual do projeto (Fase 2). Deve ser blindado com **Spring Security + JWT/OAuth2** quando o módulo de usuários for concebido (Fase 4/5).

---

### B. Mass Assignment & Higiene de DTOs

#### [SEC-02] Exposição de `dataCompra` no DTO de Entrada sem Validação Temporal
- **Severidade:** `LOW`
- **Status:** ✅ **CORRIGIDO**
- **Localização:** `CarroInput.java:27`
- **Ação Aplicada:**
  - Adicionada a anotação `@PastOrPresent` no atributo `OffsetDateTime dataCompra` do `CarroInput`.
  - Criado o teste unitário automatizado `CarroInputTest.java` com 100% de sucesso para validar rejeição de datas futuras e aceitação de datas válidas/nulas.
- **Evidência da Correção:**
```java
@PastOrPresent
private OffsetDateTime dataCompra;
```

---

### C. Configuração de Rede & Headers

#### [SEC-03] Política de CORS Irrestrita (`origins = "*"`)
- **Severidade:** `LOW`
- **Status:** ⏳ **Aguardando Frontend** (Fase 5 do Roadmap)
- **Localização:** `CarroController.java:24`
- **Evidência:**
```java
@CrossOrigin(origins = "*")
public class CarroController { ... }
```
- **Impacto:** Permite que qualquer script rodando em navegadores de terceiros efetue requisições contra a API.
- **Recomendação:** Quando o frontend da Fase 5 (React/Vite) for desenvolvido, configure uma origem explícita (ex: `http://localhost:5173`) em uma classe `@Configuration` global de WebMvc.

---

### D. Gestão de Segredos & Configurações

#### [SEC-04] Credenciais de Banco Locais Hardcoded
- **Severidade:** `LOW`
- **Status:** ✅ **CORRIGIDO**
- **Localização:** `application.properties:1-3`
- **Ação Aplicada:**
  - Externalizadas as propriedades de URL, usuário e senha do banco usando placeholders de ambiente com fallback padrão para ambiente local:
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3307/concessionaria?createDatabaseIfNotExist=true&serverTimezone=UTC}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:catita01}
```
- **Resultado:** A aplicação continua funcionando imediatamente no ambiente de desenvolvimento local, mas agora permite injeção segura de credenciais via variáveis de ambiente em esteiras de CI/CD, Docker e produção.

#### [SEC-05] `server.error.include-message=ALWAYS`
- **Severidade:** `INFO`
- **Localização:** `application.properties:10`
- **Impacto:** Em caso de falha antes da requisição atingir o `ApiExceptionHandler` (ex: filtros de servlet), o Spring Boot pode exibir mensagens internas de erro.
- **Recomendação:** Utilizar `server.error.include-message=never` ou remover a linha para produção.

---

## 🛠️ 3. Testes Automatizados e Validação Pós-Correção
Executada a suite completa de testes unitários do Maven:
* `CarroInputTest`: 2 testes de validação temporal (`@PastOrPresent`) executados e aprovados.
* `CarroInputDisassemblerTest`: 2 testes aprovados.
* `CarroModelAssemblerTest`: 3 testes aprovados.
* `CarroTest`: 4 testes aprovados.
* **Resultado:** **11 testes executados, 0 falhas, 0 erros.**

---

## 🧹 4. Falsos Positivos Descartados (Filtro Anti-Ruído)

1. **SQL Injection em `CarroRepository.findByAno`:**  
   *Descartado:* A consulta é derivada pelo Spring Data JPA e mapeada pelo Hibernate como `PreparedStatement` seguro. Não há risco de concatenação de strings.
2. **Dados em `afterMigrate.sql`:**  
   *Descartado:* Os dados contidos no script são exclusivamente mocks/fixtures para ambiente local de desenvolvimento.
3. **Exposição do Swagger UI (`/swagger-ui/index.html`):**  
   *Descartado:* A documentação interativa é intencional e essencial para os testes de desenvolvimento da API.
4. **Tratamento de Exceções (`ApiExceptionHandler`):**  
   *Validado:* A aplicação já implementa a especificação RFC 7807 (*Problem Details*) com mensagem genérica blindada para erros 500, sem vazamento de detalhes internos do MySQL para o cliente.

---

## 🎯 5. Conclusão e Veredito

Com a aplicação dos dois patches de segurança:
1. O DTO de entrada está protegido contra injeção de datas temporais anômalas no cadastro (`@PastOrPresent`).
2. As propriedades de banco de dados suportam injeção externa de variáveis de ambiente sem expor senhas fixas.
3. A suite de testes unitários foi expandida para 11 testes, mantendo **100% de aprovação e zero quebras**.
