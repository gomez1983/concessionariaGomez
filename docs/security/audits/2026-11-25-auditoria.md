# 🛡️ Relatório de Auditoria de Segurança — Concessionária Gomez

**Data da Auditoria:** 25/11/2026 - 18:06  
**Escopo Auditado:** Raiz do projeto (`c:\Projetos\concessionaria` — Backend Spring Boot, Frontend React e Infraestrutura Docker/Nginx)  
**Arquivo do Relatório:** `docs/security/audits/2026-11-25-auditoria.md`  

---

### 🔍 Stack Detectado
- **Backend:** Java 21 LTS, Spring Boot 3.3.4 (Maven Wrapper)
- **Persistência & Banco:** Spring Data JPA / Hibernate 6, MySQL 8.0, Flyway Migrations (`V001` a `V010`)
- **Módulos Chave:** Jakarta Bean Validation, Spring HATEOAS, SpringDoc OpenAPI 2.6.0, ModelMapper 3.2.1, TwelveMonkeys ImageIO WebP 3.12.0, Lombok 1.18.34
- **Frontend:** React 19.3.1, Vite 8.3.5, TypeScript 7.0.2, Tailwind CSS 4.3.3, Radix UI
- **Infraestrutura:** Docker Multi-Stage, Docker Compose, Nginx Alpine (Reverse Proxy), GitHub Actions CI
- **Mecanismos de Segurança:** Ausência de `spring-boot-starter-security`; controle de alçada comercial customizado em controller via headers HTTP (`X-Operador-Perfil` e `X-Operador-Id`)

---

## 📊 1. Resumo Executivo de Severidade

| Severidade | Quantidade | Status Principal |
| :--- | :---: | :--- |
| 🔴 **CRITICAL** | **0** | Nenhuma vulnerabilidade crítica de exploração catastrófica imediata (SQLi, RCE, Path Traversal ou chaves de produção no Git) |
| 🟠 **HIGH** | **2** | Controle de acesso via headers forjáveis (*fail-open* para `GERENTE`), mutações/PII sem autenticação e vazamento de custos/piso absoluto + entidade JPA |
| 🟡 **MEDIUM** | **2** | Bypass das regras da Fase 7 por emissão/edição direta de vendas ou injeção de `RECEITA_ADICIONAL_ELEGIVEL`, e CORS `*` nos controllers |
| 🔵 **LOW** | **3** | Vazamento de caminhos internos em `IOException`, ausência de `nosniff`/headers HTTP em foto principal e Nginx, e credenciais locais padrão |
| ⚪ **INFO** | **2** | Swagger UI/OpenAPI expostos no proxy Nginx e `spring.jpa.show-sql=true` habilitado globalmente |

---

## 🔎 2. Detalhamento dos Achados por Categoria

### A. Autenticação e Controle de Acesso (AuthN / AuthZ — OWASP API2:2023 & API5:2023)

#### [SEC-01] Controle de Alçada Baseado em Headers Forjáveis com Fallback *Fail-Open* (`GERENTE`) e Endpoints de Mutação/PII Desprotegidos
- **Severidade:** `HIGH` *(evolução do antigo `[SEC-01]` de 02/10/2026 após a entrada de PII de Clientes, Vendas e Alçadas Comerciais nas Fases 4 a 7)*
- **CWE:** CWE-290 (Authentication Bypass by Spoofing) / CWE-276 (Incorrect Default Permissions) / CWE-306 (Missing Authentication for Critical Function)
- **Localização:**
  - `src/main/java/com/concessionaria/gomez/domain/model/PerfilOperadorComercial.java:20-29`
  - `src/main/java/com/concessionaria/gomez/api/controller/PropostaComercialController.java:56-60, 138-154, 197-205`
  - `src/main/java/com/concessionaria/gomez/api/controller/CarroController.java:56-106`
  - `src/main/java/com/concessionaria/gomez/api/controller/ClienteController.java:33-83`
  - `src/main/java/com/concessionaria/gomez/api/controller/VendaController.java:33-98`
  - `frontend/src/lib/api.ts:22-34, 56-61, 196-201`
- **Evidência:**
```java
// PerfilOperadorComercial.java:20-29
public static PerfilOperadorComercial fromHeader(String raw) {
    if (raw == null || raw.isBlank()) {
        return GERENTE; // Fail-open: ausência do header concede privilégio gerencial
    }
    try {
        return PerfilOperadorComercial.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
        return VENDEDOR;
    }
}
```
```java
// PropostaComercialController.java:138-151
public PropostaComercialModel decidirAprovacao(
        @PathVariable Long propostaId,
        @RequestBody @Valid DecisaoAprovacaoInput input,
        @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader,
        @RequestHeader(value = "X-Operador-Id", required = false) String operadorId) {
    PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
    PropostaComercial proposta = gestaoPropostaService.decidirAprovacaoGerencial(
            propostaId, input.getSequenciaRevisao(), operadorId, perfil, ...);
```
- **Impacto:**
  1. **Escalação trivial de privilégio e bypass de anti-autoaprovação:** Qualquer requisitante HTTP (ou usuário alterando o header) pode enviar `X-Operador-Perfil: ADMIN` e forjar outro `X-Operador-Id` para aprovar a própria proposta comercial fora da autonomia (`GestaoPropostaComercialService.java:160`), cadastrar novas políticas comerciais (`POST /propostas/politicas`) ou visualizar margens restritas.
  2. **Default inseguro (*fail-open*):** Requisições sem o cabeçalho `X-Operador-Perfil` recebem automaticamente o perfil `GERENTE`.
  3. **Exposição de PII e mutações críticas sem autenticação:** Endpoints como `GET /clientes` (CPF/CNPJ, telefone, e-mail e endereço completo), `DELETE /clientes/{id}`, `DELETE /carros/{id}` e `DELETE /vendas/{id}` são acessíveis sem credenciais.
- **Recomendação:** Implementar **Spring Security + JWT stateless (Fase 8)** extraindo identidade e `PerfilOperadorComercial` exclusivamente do `SecurityContext`. Como contenção imediata antes da Fase 8, alterar o fallback de `fromHeader` para *fail-closed* (`VENDEDOR`).

---

### B. Exposição Excessiva de Dados & Entidade JPA Exposta (OWASP API3:2023 — BOPLA)

#### [SEC-02] Vazamento de Custo de Aquisição (`compra`), Piso Absoluto e Retorno Direto da Entidade JPA `PoliticaComercial`
- **Severidade:** `HIGH`
- **CWE:** CWE-200 (Exposure of Sensitive Information) / CWE-213
- **Localização:**
  - `src/main/java/com/concessionaria/gomez/api/controller/PropostaComercialController.java:180-229`
  - `src/main/java/com/concessionaria/gomez/domain/model/PoliticaComercial.java:27-41`
  - `src/main/java/com/concessionaria/gomez/api/model/CarroModel.java:20`
  - `src/main/java/com/concessionaria/gomez/api/controller/CarroController.java:39-54`
  - `src/main/java/com/concessionaria/gomez/infrastructure/repository/spec/CarroSpecs.java:39-45`
- **Evidência:**
```java
// PropostaComercialController.java:180-192 — retorna Entidade JPA direta e sem checar perfil
@GetMapping("/politica-vigente")
public PoliticaComercial consultarPoliticaVigente(@RequestParam(required = false) Long carroId) {
    ...
    var padroes = politicaRepository.findVigentes(ContextoPoliticaComercial.LOJA_PADRAO, null, agora);
    return padroes.isEmpty() ? null : padroes.get(0);
}
```
```java
// CarroModel.java:20 — serializado em GET /carros e GET /carros/{id} para qualquer perfil
private BigDecimal compra;
```
- **Impacto:**
  1. Embora `/propostas` e `/dashboard` ocultem cuidadosamente `custoAquisicao`, `margemContribuicaoEstimada` e `pisoAbsoluto` para `VENDEDOR`, qualquer operador `VENDEDOR` obtém o **custo exato de compra do veículo (`compra`)** em `GET /carros` (e pode inferi-lo via filtros `minValor`/`maxValor` em `CarroSpecs`) e obtém a **`margemPisoAbsoluto`** em `GET /propostas/politica-vigente`, reconstruindo integralmente o piso confidencial da loja.
  2. `GET /propostas/politica-vigente` e `POST /propostas/politicas` retornam a entidade JPA `PoliticaComercial` sem DTO de saída. Quando há política `EXCECAO_VEICULO` (`carro != null` com `FetchType.LAZY`), a serialização Jackson expõe o grafo interno da entidade `Carro` ou dispara erro 500 de proxy Hibernate (`ByteBuddyInterceptor`).
- **Recomendação:** Criar `PoliticaComercialModel` (DTO de saída), restringir `margemPisoAbsoluto` e `compra` (em `CarroModelAssembler`) apenas a operadores autorizados (`podeVerCustosEMargensInternas()`), e alinhar o filtro público `minValor`/`maxValor` ao preço anunciado/sugerido para perfis não gerenciais.

---

### C. Integridade de Regras de Negócio e Fluxo Financeiro (CWE-840)

#### [SEC-03] Bypass da Política Comercial, Reserva e Piso Absoluto via Venda Direta (`POST /vendas` / `PUT /vendas/{id}`) e `itensExtras` Elegíveis
- **Severidade:** `MEDIUM`
- **CWE:** CWE-840 (Business Logic Errors)
- **Localização:**
  - `src/main/java/com/concessionaria/gomez/api/controller/VendaController.java:50-79`
  - `src/main/java/com/concessionaria/gomez/domain/service/EmissaoVendaService.java:28-99`
  - `src/main/java/com/concessionaria/gomez/api/model/input/PropostaComercialInput.java:52-62`
  - `src/main/java/com/concessionaria/gomez/domain/service/MotorFinanceiroComercialService.java:166-168`
- **Evidência:**
```java
// VendaController.java:52-64 — permite emitir venda sem proposta aprovada por qualquer valor > 0
if (vendaInput.getPropostaId() != null && gestaoPropostaService != null) {
    venda = gestaoPropostaService.converterEmVenda(...);
} else {
    venda = emissaoVendaService.realizarVenda(
            vendaInput.getCarroId(), vendaInput.getClienteId(),
            vendaInput.getValorVenda(), vendaInput.getObservacoes());
}
```
```java
// MotorFinanceiroComercialService.java:166-168 — aceita RECEITA_ADICIONAL_ELEGIVEL confirmada vinda do input
} else if (item.getBaseCalculo() == BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL) {
    b = b.add(valorItem.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP));
}
```
- **Impacto:**
  1. **Atalho de venda legada e edição pós-aprovação:** Chamar `POST /vendas` sem `propostaId` (ou `PUT /vendas/{id}` após a conversão) permite fechar ou alterar uma venda com qualquer `valorVenda > 0`, ignorando `PoliticaComercial`, `margemPisoAbsoluto` e reservas ativas (`EmissaoVendaService` checa apenas `carro.getAtivo()`, não `DisponibilidadeComercial.RESERVADO`).
  2. **Inflação de receita acessória pelo vendedor:** Em `POST /propostas`, o vendedor pode enviar `itensExtras` com `baseCalculo = RECEITA_ADICIONAL_ELEGIVEL` e `confirmado = true` no payload, reduzindo artificialmente o numerador `(C + F - B)` do `pisoAutonomia` e do `pisoAbsoluto` para obter aprovação automática (`APROVADA_POLITICA_AUTOMATICA`).
- **Recomendação:** Restringir emissão direta sem proposta e edição `PUT /vendas/{id}` exclusivamente a `GERENTE`/`ADMIN` (validando também reservas ativas em `EmissaoVendaService`), e forçar `confirmado = false` (ou exigir alçada gerencial) quando um `VENDEDOR` incluir itens `RECEITA_ADICIONAL_ELEGIVEL` / `BONUS_CAMPANHA_MONTADORA` manualmente no payload.

---

### D. Configuração de Rede, Tratamento de Erros e Hardening

#### [SEC-04] Política de CORS Irrestrita (`@CrossOrigin(origins = "*")`) Descentralizada nos Controllers
- **Severidade:** `MEDIUM`
- **CWE:** CWE-942 (Permissive Cross-domain Policy)
- **Localização:** `CarroController.java:28`, `CarroFotoController.java:30`, `ClienteController.java:24`, `DashboardController.java:11`, `PropostaComercialController.java:38`, `VendaController.java:24` *(ausente em `CarroGaleriaController.java`)*
- **Impacto:** Qualquer página de terceiros aberta no navegador de um funcionário na rede interna pode disparar requisições cross-origin contra `localhost:8080` (especialmente crítico porque o backend ainda não exige credenciais por origem restrita e há inconsistência entre `/foto` e `/fotos`).
- **Recomendação:** Remover `@CrossOrigin(origins = "*")` dos controllers e centralizar o CORS em configuração global (`WebMvcConfigurer` / `SecurityFilterChain`) parametrizada por variável de ambiente (ex.: `CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:5173`).

#### [SEC-05] Vazamento de Caminhos de Disco em `IOException` (`NegocioException`) e `printStackTrace()`
- **Severidade:** `LOW`
- **CWE:** CWE-209 (Generation of Error Message Containing Sensitive Information)
- **Localização:** `DiscoLocalFotoStorageService.java:27, 37, 56`, `CarroFotoController.java:57`, `ApiExceptionHandler.java:85`, `src/main/resources/application.properties:13`
- **Evidência:**
```java
// DiscoLocalFotoStorageService.java:26-28
} catch (Exception e) {
    throw new NegocioException("Não foi possível armazenar o arquivo da foto: " + e.getMessage());
}
```
- **Impacto:** Como `ApiExceptionHandler.handleNegocio` devolve `ex.getMessage()` diretamente no campo `userMessage`/`detail` da resposta `400`, falhas de E/S (`FileSystemException` / `AccessDeniedException`) expõem o caminho físico completo do servidor (`/app/catalogo-fotos/...`). Além disso, `ApiExceptionHandler.java:85` usa `ex.printStackTrace()` em vez de logger estruturado SLF4J, e `application.properties:13` mantém `server.error.include-message=ALWAYS`.
- **Recomendação:** Registrar a exceção real via `Logger` SLF4J no servidor e retornar mensagem estática amigável ao cliente (como já é feito corretamente em `CarroGaleriaController.java:65`); configurar `server.error.include-message=never` para produção.

#### [SEC-06] Ausência de `X-Content-Type-Options: nosniff` em `CarroFotoController` e Headers de Segurança no Nginx
- **Severidade:** `LOW`
- **CWE:** CWE-693 (Protection Mechanism Failure)
- **Localização:** `src/main/java/com/concessionaria/gomez/api/controller/CarroFotoController.java:81-84`, `frontend/nginx.conf:1-65`
- **Impacto:** `CarroGaleriaController.java:52` já aplica `.header("X-Content-Type-Options", "nosniff")`, mas `CarroFotoController.servirFoto` não inclui esse cabeçalho. No `frontend/nginx.conf`, faltam cabeçalhos básicos de defesa em profundidade (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` e `Referrer-Policy: strict-origin-when-cross-origin`).
- **Recomendação:** Adicionar `X-Content-Type-Options: nosniff` (e `Content-Disposition` sanitizado, já que o nome salvo é UUID) em `CarroFotoController` e declarar os headers de segurança no bloco `server` do `nginx.conf`.

#### [SEC-07] Credenciais Padrão em `docker-compose.yml` / `application.properties` e Container Nginx em Root
- **Severidade:** `LOW`
- **CWE:** CWE-798 / CWE-250
- **Localização:** `docker-compose.yml:7, 14, 32`, `src/main/resources/application.properties:3`, `frontend/Dockerfile:18`
- **Impacto:** Adequado para desenvolvimento local rápido, mas `docker-compose.yml` possui senha fixa (`<senha-local-padrao>`, omitida neste relatório por o repositório ser público) sem interpolação `${MYSQL_ROOT_PASSWORD}` e expõe a porta `3307:3306` em todas as interfaces (`0.0.0.0`). Enquanto o `Dockerfile` do backend já roda de forma exemplar com `USER appuser`, o `frontend/Dockerfile` usa `nginx:alpine` padrão (root) em vez de `nginxinc/nginx-unprivileged:alpine` ou vínculo local `127.0.0.1:3307:3306`.
- **Recomendação:** Parametrizar `docker-compose.yml` via `.env` (com `.env.example` versionado), restringir o bind do MySQL a `127.0.0.1:3307:3306` e exigir `DB_PASSWORD` explícito em profile `prod`.

---

### E. Observações Arquiteturais e Ambiente Produtivo (INFO)

#### [SEC-08] Swagger UI e `/v3/api-docs` Publicados no Proxy Reverso Nginx
- **Severidade:** `INFO`
- **Localização:** `frontend/nginx.conf:39-53`
- **Recomendação:** Excelente para desenvolvimento e homologação; ao criar o profile `application-prod.properties`, desabilitar a exposição pública com `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` (ou exigir papel `ADMIN` no Spring Security).

#### [SEC-09] `spring.jpa.show-sql=true` Ativo Globalmente
- **Severidade:** `INFO`
- **Localização:** `src/main/resources/application.properties:10`
- **Recomendação:** Mover `spring.jpa.show-sql=true` para `application-dev.properties` para evitar degradação de I/O e vazamento indireto de parâmetros/queries em logs de produção.

---

## 🛠️ 3. Propostas de Correção (Patches Sugeridos para Achados HIGH)

> ⚠️ **Revise cada patch antes de aplicar. Nada foi alterado no repositório.**

### Patch 1 — `[SEC-01]` Contenção imediata *Fail-Closed* em `PerfilOperadorComercial` (pré-Fase 8 com Spring Security + JWT)

```diff
--- a/src/main/java/com/concessionaria/gomez/domain/model/PerfilOperadorComercial.java
+++ b/src/main/java/com/concessionaria/gomez/domain/model/PerfilOperadorComercial.java
@@ -18,8 +18,8 @@ public enum PerfilOperadorComercial {
     }
 
     public static PerfilOperadorComercial fromHeader(String raw) {
         if (raw == null || raw.isBlank()) {
-            return GERENTE;
+            return VENDEDOR;
         }
         try {
             return PerfilOperadorComercial.valueOf(raw.trim().toUpperCase());
```

*(Nota arquitetural: a correção definitiva de `[SEC-01]` é a implementação da **Fase 8** com `spring-boot-starter-security`, filtro JWT stateless e extração do operador autenticado via `@AuthenticationPrincipal`, eliminando completamente a leitura de `X-Operador-Perfil` e `X-Operador-Id`.)*

### Patch 2 — `[SEC-02]` Ocultação de `compra` por Perfil e Substituição da Entidade JPA `PoliticaComercial` por DTO Seguro

```diff
--- a/src/main/java/com/concessionaria/gomez/api/assembler/CarroModelAssembler.java
+++ b/src/main/java/com/concessionaria/gomez/api/assembler/CarroModelAssembler.java
@@ -5,6 +5,7 @@ import com.concessionaria.gomez.api.controller.CarroFotoController;
 import com.concessionaria.gomez.api.controller.CarroGaleriaController;
 import com.concessionaria.gomez.api.model.CarroModel;
 import com.concessionaria.gomez.domain.model.Carro;
+import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
 import lombok.RequiredArgsConstructor;
 import org.modelmapper.ModelMapper;
 import org.springframework.stereotype.Component;
@@ -23,11 +24,18 @@ public class CarroModelAssembler {
     private final ModelMapper modelMapper;
 
     public CarroModel toModel(Carro carro) {
+        return toModel(carro, PerfilOperadorComercial.VENDEDOR);
+    }
+
+    public CarroModel toModel(Carro carro, PerfilOperadorComercial perfil) {
         if (carro == null) {
             return null;
         }
 
         CarroModel carroModel = modelMapper.map(carro, CarroModel.class);
+        if (perfil == null || !perfil.podeVerCustosEMargensInternas()) {
+            carroModel.setCompra(null);
+        }
         carroModel.setVenda(carro.calcularPrecoVendaSugerido());
```

```diff
--- a/src/main/java/com/concessionaria/gomez/api/controller/PropostaComercialController.java
+++ b/src/main/java/com/concessionaria/gomez/api/controller/PropostaComercialController.java
@@ -179,7 +179,13 @@ public class PropostaComercialController {
 
     @GetMapping("/politica-vigente")
     @Operation(summary = "Consulta a política comercial vigente para a loja ou para um veículo específico")
-    public PoliticaComercial consultarPoliticaVigente(@RequestParam(required = false) Long carroId) {
+    public PoliticaComercialModel consultarPoliticaVigente(
+            @RequestParam(required = false) Long carroId,
+            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader) {
+        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
+        if (!perfil.podeVerCustosEMargensInternas()) {
+            throw new AcessoComercialNegadoException("Apenas GERENTE ou ADMIN podem consultar margens internas da política comercial.");
+        }
         OffsetDateTime agora = OffsetDateTime.now();
         if (carroId != null) {
             var excecoes = politicaRepository.findVigentes(ContextoPoliticaComercial.EXCECAO_VEICULO, carroId, agora);
             if (!excecoes.isEmpty()) {
-                return excecoes.get(0);
+                return PoliticaComercialModel.from(excecoes.get(0));
             }
         }
         var padroes = politicaRepository.findVigentes(ContextoPoliticaComercial.LOJA_PADRAO, null, agora);
-        return padroes.isEmpty() ? null : padroes.get(0);
+        return padroes.isEmpty() ? null : PoliticaComercialModel.from(padroes.get(0));
     }
```

---

## 📦 4. Análise de Dependências e CVEs
- **Backend (`pom.xml`):** Spring Boot `3.3.4`, Java `21`, SpringDoc `2.6.0`, ModelMapper `3.2.1`, TwelveMonkeys `3.12.0` e Lombok `1.18.34` — nenhuma CVE crítica conhecida afetando os fluxos utilizados no código atual (Jackson está blindado com `fail-on-unknown-properties=true` e sem polimorfismo `enableDefaultTyping` inseguro).
- **Frontend (`frontend/package-lock.json`):** Executado `npm audit --omit=dev` sobre as 37 dependências de produção — **0 vulnerabilidades encontradas** (`critical: 0, high: 0, moderate: 0, low: 0`).

---

## 🔑 5. Varredura de Segredos e Arquivos Rastreados
- **Arquivos sensíveis rastreados no Git (`git ls-files`):** **Nenhum arquivo sensível detectado** (nenhum `.env`, `*.pem`, `*.key`, `*.jks` ou `credentials.json` versionado; `frontend/.env.example` contém apenas exemplo público).
- **Credenciais em arquivos de configuração:** `application.properties` utiliza variáveis de ambiente (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) com fallback local (`<senha-local-padrao>`, omitido por o repositório ser público) compartilhado com `docker-compose.yml` e `.github/workflows/ci.yml` (registrado em `[SEC-07]`).

---

## 🧹 6. Falsos Positivos Descartados
Elementos inspecionados e **descartados** após rastreamento completo de fluxo (*Taint Analysis*):
1. **SQL / JPQL Injection em Repositórios e Specifications:** `CarroRepository`, `ClienteRepository`, `PoliticaComercialRepository`, `VendaRepository`, `CarroSpecs` e `PropostaComercialController` utilizam exclusivamente parâmetros nomeados (`:param`) com `PreparedStatement` ou `CriteriaBuilder` tipado.
2. **Upload Malicioso, Polyglot Image, Image Bomb e Path Traversal (`FotoCarroInput` / `ImagemValidator` / `DiscoLocalFotoStorageService`):** Implementação exemplar — `ImagemValidator` valida tanto o `Content-Type` quanto os bytes reais da imagem via `ImageIO`, restringe dimensões máximas (`12000x12000` e `40 MP`), `ArquivoFotoTransacionalService` ignora o `originalFilename` gerando `UUID`, e `DiscoLocalFotoStorageService` valida `arquivo.startsWith(raiz)`.
3. **Cross-Site Scripting (XSS) e Sessão no Frontend React:** Nenhum uso de `dangerouslySetInnerHTML`, `innerHTML` ou `eval`. O `localStorage` persiste apenas o tema visual (`gomez:theme`), `cep.ts` sanitiza o CEP com `/^\d{8}$/` antes de consultar o ViaCEP via HTTPS com timeout, e `VITE_API_BASE_URL` é configuração pública legítima.
4. **Mass Assignment em `CarroInput` e `ClienteInput`:** DTOs dedicados sem `id`/`ativo`, `@PastOrPresent` em `dataCompra`, `setSkipNullEnabled(true)` no `ModelMapper` e `fail-on-unknown-properties=true` global no Jackson.
5. **Dados em `src/test/resources/db/testdata/afterMigrate.sql` e `src/test/java`:** Fixtures fictícias isoladas no escopo de teste.

---

## 🎯 7. Conclusão e Próximos Passos
A base de código apresenta **alta qualidade de engenharia** em validação de entrada, prevenção contra SQL Injection, upload seguro de imagens com sincronização transacional e segurança do frontend React. Os riscos reais concentraram-se exatamente na fronteira de **identidade, autorização e proteção de dados comerciais internos** introduzida com a expansão funcional das Fases 4 a 7:

1. **Prioridade 1 (Fase 8 — AuthN/AuthZ):** Implementar `spring-boot-starter-security` + JWT stateless + RBAC real (`VENDEDOR`, `GERENTE`, `ADMIN`), eliminando os headers `X-Operador-Perfil` / `X-Operador-Id` e protegendo `/clientes`, `/carros`, `/vendas` e `/propostas`.
2. **Prioridade 2 (Confidencialidade Comercial — `[SEC-02]` e `[SEC-03]`):** Criar `PoliticaComercialModel`, ocultar `compra` em `CarroModel` para `VENDEDOR`, exigir alçada gerencial para vendas/edições diretas sem proposta e impedir auto-confirmação de `RECEITA_ADICIONAL_ELEGIVEL` pelo vendedor.
3. **Prioridade 3 (Hardening de Borda — `[SEC-04]` a `[SEC-07]`):** Centralizar CORS por ambiente, sanitizar mensagens de `IOException` no storage, adicionar `nosniff`/headers HTTP em `CarroFotoController` e `nginx.conf`, e separar configurações de `dev`/`prod`.

---

## 🤝 8. Revisão Cruzada e Consolidação com a Auditoria Independente (Claude)

> Referência cruzada com [`2026-11-25-auditoria-claude.md`](2026-11-25-auditoria-claude.md), executada de forma independente sobre a mesma base de código para combinar o melhor das duas análises.

### 8.1 Convergências Principais (Alta Confiança)
Ambas as auditorias chegaram de forma independente ao mesmo diagnóstico estrutural:
- **Zero vulnerabilidades críticas clássicas:** ausência de SQL/JPQL Injection, upload de imagens blindado (`ImagemValidator` + `UUID` + anti-*path traversal*), container do backend executando sem privilégios (`appuser`) e `0` vulnerabilidades no `npm audit --omit=dev`.
- **Fronteira crítica de identidade e alçada:** `PerfilOperadorComercial.fromHeader` em *fail-open* (`GERENTE`), ausência de `spring-boot-starter-security` expondo PII de clientes (`/clientes`) e mutações na rede, e governança da Fase 7 apoiada em cabeçalhos `X-Operador-*` controlados pelo cliente.
- **Exposição comercial indevida e bypass em vendas:** vazamento de `compra` em `/carros` e da entidade JPA `PoliticaComercial` em `/propostas/politica-vigente`, além do contorno de política/reserva via `POST /vendas` direto e `PUT /vendas/{id}`.

### 8.2 Achados Exclusivos do Claude Incorporados ao Plano
1. **Venda Dupla via `DELETE /vendas/{id}` após Cancelamento Auditado (`SEC-05` do Claude):**
   - Em `EmissaoVendaService.cancelar`, excluir fisicamente uma venda A que já havia sido cancelada com auditoria executa `carro.ativar()` sem verificar `venda.getCancelada()` nem se o veículo já possui outra venda B vigente (`VENDIDO`), reabrindo indevidamente um veículo já revendido.
2. **CVE Aplicável no Tomcat Embarcado e Fim de Suporte do Spring Boot `3.3.x` (`SEC-08` do Claude):**
   - O `spring-boot-starter-parent` `3.3.4` resolve `tomcat-embed-core` `10.1.30`, afetado pela **CVE-2025-48988** (DoS em upload `multipart/form-data`, aplicável pois `/carros/{id}/foto` e `/fotos` aceitam multipart sem autenticação; corrigido no Tomcat `10.1.42`). Também confirmou o fim de suporte da linha `3.3.x` e descartou com precisão CVEs não aplicáveis de `spring-webmvc`/`spring-context` e XXE no Woodstox.
3. **Canal Lateral de Ordenação (`?sort=compra`):**
   - Mesmo omitindo `compra` no JSON para `VENDEDOR`, `GET /carros?sort=compra,asc` via `Pageable` permitiria ordenar veículos pelo custo confidencial de aquisição caso as propriedades de ordenação não sejam restringidas por perfil.
4. **Hardening de Infraestrutura e Higiene (`SEC-07`, `SEC-10`, `SEC-11` do Claude):**
   - Conexão da aplicação ao MySQL como `root` (recomenda-se usuário dedicado restrito ao schema `concessionaria`), omissão de credenciais literais em relatórios de repositório público, `server_tokens off` no Nginx e atenção à **regra de herança do Nginx**: blocos `location` que declaram `add_header` (como `/index.html` e assets estáticos em `frontend/nginx.conf`) anulam os `add_header` herdados do bloco `server {}`.

### 8.3 Achados Exclusivos desta Auditoria que Complementam a do Claude
1. **Bypass de Alçada e Piso Absoluto via `itensExtras` na Proposta (`[SEC-03]`):**
   - Em `PropostaComercialInput.ItemFinanceiroInput` + `MotorFinanceiroComercialService.java:166-168`, um `VENDEDOR` pode enviar `baseCalculo = RECEITA_ADICIONAL_ELEGIVEL` com `confirmado = true` em `POST /propostas`. Isso infla `+ B` e reduz o numerador `(C + F - B)` do `pisoAutonomia` e do `pisoAbsoluto` (podendo zerar o piso matemático), obtendo `APROVADA_POLITICA_AUTOMATICA` sem aprovação gerencial mesmo após a implantação do Spring Security.
2. **Inferência de Custo via Filtros `minValor` / `maxValor` em `CarroSpecs` (`[SEC-02]`):**
   - Além do `?sort=compra` apontado pelo Claude, `CarroSpecs.java:39-45` aplica `minValor` e `maxValor` diretamente sobre `root.get("compra")`, permitindo descobrir o custo exato de qualquer veículo por busca binária mesmo com `compra = null` no DTO. Para `VENDEDOR`, o filtro deve operar sobre o preço anunciado/sugerido.
3. **Risco de Erro `500` por Proxy `LAZY` em `PoliticaComercial`, `operadorOverrideId` no Frontend e CORS Assimétrico:**
   - Expor `PoliticaComercial` sem DTO quando `contexto = EXCECAO_VEICULO` aciona serialização de proxy Hibernate `FetchType.LAZY` (`Carro`), podendo causar `500 Internal Server Error`; mapeado também o parâmetro `operadorOverrideId` em `frontend/src/lib/api.ts:196-201` e a ausência de `@CrossOrigin` em `CarroGaleriaController` frente aos demais 6 controllers.

### 8.4 Ressalvas Técnicas aos Patches Sugeridos pelo Claude (Cuidados na Aplicação)
1. **CSP do Nginx (`SEC-10` do Claude) vs. ViaCEP, Tema Inicial e Swagger UI:**
   - A política sugerida (`default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'`) **bloquearia a consulta externa ao ViaCEP** (`https://viacep.com.br` em `frontend/src/lib/cep.ts:14`) por ausência de `connect-src 'self' https://viacep.com.br`, além de bloquear o `<script>` inline de prevenção de *flash* de tema em `frontend/index.html:12-19` e scripts inline do `/swagger-ui/`. A CSP deve incluir `connect-src 'self' https://viacep.com.br`, externalizar (ou assinar via hash SHA-256) o script de tema e não restringir a rota `/swagger-ui/` em desenvolvimento.
2. **Permissão em `POST /propostas/*/converter-venda` (`SecurityConfig` do Claude):**
   - Restringir `converter-venda` exclusivamente a `GERENTE` e `ADMIN` impediria que um `VENDEDOR` concluísse o fechamento de propostas já aprovadas dentro da sua própria autonomia (`DENTRO_AUTONOMIA`) ou já deferidas pela gerência. O correto é permitir `VENDEDOR` autenticado em `converter-venda` (pois `GestaoPropostaComercialService` já exige status `APROVADA` e valida validade/custo), restringindo a `GERENTE`/`ADMIN` apenas a venda direta sem proposta (`POST /vendas`), `PUT /vendas/{id}` e cancelamentos.
3. **Salto Direto para Spring Boot `3.5.x` (`SEC-08` do Claude):**
   - A atualização é recomendada, mas saltar de `3.3.4` direto para `3.5.x` envolve Hibernate `6.6`, nova linha do Flyway e depreciações de `@MockBean` nos testes. Recomenda-se fixar `<tomcat.version>10.1.42</tomcat.version>` como mitigação imediata sem risco ou executar o upgrade do Spring Boot em commit/etapa isolada validada com `mvnw clean verify` antes de iniciar o Spring Security.

### 8.5 Roteiro Unificado ("Melhor dos Dois Mundos")
1. **Etapa 1 — Contenção Imediata e Dependências:**
   - Bind local `127.0.0.1` nas portas do `docker-compose.yml`, credenciais em `.env` + `.env.example`, usuário MySQL dedicado (não-`root`), fallback *fail-closed* (`VENDEDOR`) em `PerfilOperadorComercial.fromHeader` e correção da `CVE-2025-48988` (via `<tomcat.version>` ou upgrade validado do Spring Boot).
2. **Etapa 2 — Blindagem de Domínio e Confidencialidade Comercial:**
   - `PoliticaComercialModel` (DTO), ocultação de `compra` em `CarroModelAssembler` para `VENDEDOR`, bloqueio de `?sort=compra` + ajuste de `minValor`/`maxValor` para não filtrar `compra` em perfil `VENDEDOR`.
   - Bloqueio de auto-confirmação de `RECEITA_ADICIONAL_ELEGIVEL` por `VENDEDOR` em `PropostaComercial` e blindagem completa de `EmissaoVendaService` (reservas `RESERVADO`, bloqueio de `PUT` em vendas convertidas/canceladas e prevenção de venda dupla no `DELETE`).
3. **Etapa 3 — Fase 8 (Spring Security + JWT + RBAC + CORS Global):**
   - Autenticação stateless real (`VENDEDOR`, `GERENTE`, `ADMIN`), remoção definitiva dos cabeçalhos `X-Operador-*` no backend e frontend, permissão correta em `converter-venda` e política global de CORS por ambiente.
4. **Etapa 4 — Hardening Final de Borda e Observabilidade:**
   - Sanitização de mensagens `IOException` no storage + logger SLF4J no lugar de `printStackTrace()`, separação de profiles `dev`/`prod` (`show-sql`, `include-message`, Swagger) e headers de segurança em `CarroFotoController` e `nginx.conf` (com CSP compatível com ViaCEP e repetição nos blocos `location` que usam `add_header`).
