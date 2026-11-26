# 🛡️ Relatório de Auditoria de Segurança — Concessionária Gomez API

**Data da Auditoria:** 25/11/2026 - 18:07
**Escopo Auditado:** raiz do projeto (backend `src/main`, `frontend/`, `docker-compose.yml`, `Dockerfile`s, `nginx.conf`, CI)
**Arquivo do Relatório:** `docs/security/audits/2026-11-25-auditoria-claude.md`
**Auditor:** Claude (Claude Code), executando a skill `security-audit` do projeto
**Commit auditado:** `bbb30fe` (master)
**Revisão:** 25/11/2026 — atualizado após revisão cruzada com a auditoria paralela do Gemini ([`2026-11-25-auditoria.md`](2026-11-25-auditoria.md)). Ver **seção 8**.

> Auditoria anterior: [`2026-10-02-auditoria.md`](2026-10-02-auditoria.md). Desde então entraram as Fases 5 (Frontend), 6 (Docker) e 7 (Propostas Comerciais), que são o foco principal deste relatório.

---

### 🔍 Stack Detectado
- **Backend:** Java 21, Spring Boot 3.3.4 (Maven) — Spring Framework 6.1.13, Tomcat embarcado 10.1.30
- **Persistência & Banco:** Spring Data JPA / Hibernate 6.5.3, MySQL 8.0 (`mysql-connector-j` 8.3.0), Flyway 10.10.0
- **Documentação de API:** SpringDoc OpenAPI 2.6.0 (Swagger UI 5.17.14)
- **Outros módulos:** ModelMapper 3.2.1, Jackson 2.17.2 (+ `jackson-dataformat-xml`), TwelveMonkeys ImageIO WebP 3.12.0, Logback 1.5.8
- **Frontend:** React 19.3.1, Vite 8.3.5, TypeScript 7.0.2, servido por Nginx (alpine) com proxy reverso `/api` → backend
- **Infraestrutura:** Docker Compose com 3 serviços (`mysql`, `backend`, `frontend`)
- **Mecanismos de Segurança:** **Nenhum framework de autenticação** (sem Spring Security). Perfil e identidade do operador são lidos de cabeçalhos HTTP (`X-Operador-Perfil`, `X-Operador-Id`) enviados pelo cliente.

---

## 📊 1. Resumo Executivo de Severidade

> 🔁 **Tabela revisada após a revisão cruzada (seção 8).** Contagem original: HIGH 3, MEDIUM 5, LOW 3, INFO 2. Mudanças: SEC-04 elevado de MEDIUM para HIGH; incluídos SEC-14 (HIGH), SEC-15 (MEDIUM) e SEC-16 (LOW), identificados pelo Gemini e confirmados no código.

| Severidade | Quantidade | Status Principal |
| :--- | :---: | :--- |
| 🔴 **CRITICAL** | 0 | — |
| 🟠 **HIGH** | 5 | Perfil forjável com padrão `GERENTE` (SEC-01); API sem autenticação acessível pela rede (SEC-02); alçadas contornáveis via `/vendas` (SEC-03); custos e margens expostos, inclusive por filtros (SEC-04 ↑); piso reduzido por receita adicional declarada pelo vendedor (SEC-14 🆕) |
| 🟡 **MEDIUM** | 5 | Venda dupla via `DELETE /vendas` (SEC-05); CORS `*` (SEC-06); MySQL exposto com senha pública (SEC-07); CVE aplicável no Tomcat e Spring Boot sem suporte (SEC-08); entidade `PoliticaComercial` sem DTO, com possível erro 500 (SEC-15 🆕) |
| 🔵 **LOW** | 4 | Mensagens internas no erro (SEC-09); cabeçalhos de segurança no Nginx (SEC-10); aplicação conecta como `root` (SEC-11); container Nginx como `root` (SEC-16 🆕) |
| ⚪ **INFO** | 2 | Swagger exposto pela porta 80 (SEC-12); `show-sql` e `printStackTrace` (SEC-13) |

### Situação dos achados da auditoria anterior (02/10/2026)

| ID anterior | Achado | Situação hoje |
| :--- | :--- | :--- |
| SEC-01 | Endpoints sem autenticação | **Aberto e agravado** → ver **SEC-02** (agora com PII, dados financeiros e exposição na rede via Docker) |
| SEC-02 | `dataCompra` sem validação temporal | ✅ Corrigido (`@PastOrPresent` confirmado em `CarroInput`) |
| SEC-03 | CORS `origins = "*"` | **Aberto e ampliado** (agora em 6 controllers) → ver **SEC-06** |
| SEC-04 | Credenciais locais hardcoded | **Reaberto** → ver **SEC-07** (repositório é **público** e o MySQL escuta em todas as interfaces) |
| SEC-05 | `server.error.include-message=ALWAYS` | **Aberto** → incorporado em **SEC-09** |

---

## 🔎 2. Detalhamento dos Achados por Categoria

### A. Autenticação e Controle de Acesso (AuthN / AuthZ)

#### [SEC-01] Autorização da Fase 7 baseada em cabeçalho do cliente, com padrão `GERENTE` quando ausente (fail-open)
- **Severidade:** `HIGH`
- **Localização:**
  - `src/main/java/com/concessionaria/gomez/domain/model/PerfilOperadorComercial.java:20-22`
  - `src/main/java/com/concessionaria/gomez/api/controller/PropostaComercialController.java:136` (aprovações), `:168` (conversão), `:194` (políticas)
  - `src/main/java/com/concessionaria/gomez/api/controller/DashboardController.java:18`
  - `src/main/java/com/concessionaria/gomez/domain/service/GestaoPropostaComercialService.java:160` e `:190`
  - `frontend/src/lib/api.ts` (`currentPerfil = 'GERENTE'`)
- **Evidência:**
```java
// PerfilOperadorComercial.java
public static PerfilOperadorComercial fromHeader(String raw) {
    if (raw == null || raw.isBlank()) {
        return GERENTE;               // ausência de cabeçalho concede perfil gerencial
    }
    ...
}

// GestaoPropostaComercialService.java:160 — "autoaprovação" comparada com cabeçalho livre
if (decisorAutenticado != null && decisorAutenticado.trim().equalsIgnoreCase(proposta.getVendedorIdentificador())) { ... }

// GestaoPropostaComercialService.java:190 — decisor nulo vira "gerente@..." na trilha de auditoria
: "gerente@concessionariagomez.com.br");
```
- **Verificado em execução (somente leitura, via Nginx na porta 80):**
  - `GET /api/dashboard` **sem cabeçalho** → retorna `valorInvestido` e `margemPrevista` (visão de GERENTE).
  - `GET /api/dashboard` com `X-Operador-Perfil: VENDEDOR` → esses campos são omitidos.
- **Impacto:**
  - Qualquer chamada direta à API (Postman, `curl`, script, outro site) é tratada como **GERENTE** por omissão.
  - Um vendedor pode aprovar a própria proposta fora da alçada: basta enviar `X-Operador-Perfil: GERENTE` e um `X-Operador-Id` diferente do seu. A checagem de "conflito de interesse" compara apenas com um texto que o próprio cliente define.
  - A trilha de auditoria (`DecisaoAprovacao.decisorAutenticado`, `Venda.responsavelCancelamento`) registra o que o cliente declarar, ou um e-mail fixo de gerente quando nada é enviado. O registro **não é confiável como evidência**, apesar do nome "decisorAutenticado".
  - `POST /propostas/{id}/converter-venda` não verifica perfil algum.
  - 🔁 **Evidência adicional (revisão cruzada, §8.2):** a própria tela de aprovação (`frontend/src/components/Proposals.tsx:120-131`) troca a identidade do decisor por um e-mail fictício de "diretoria" quando o operador é o vendedor da proposta. A proteção contra autoaprovação é anulada no uso **normal** da aplicação, sem nenhum ataque.
- **Recomendação:**
  1. **Imediato (mitigação):** inverter o padrão para o **menor privilégio** (`VENDEDOR`) quando o cabeçalho estiver ausente ou inválido.
  2. **Definitivo:** com a introdução do Spring Security (SEC-02), **remover os cabeçalhos `X-Operador-*`** e derivar perfil e identidade exclusivamente do usuário autenticado (`Authentication`/`@AuthenticationPrincipal`), usando papéis (`ROLE_VENDEDOR`, `ROLE_GERENTE`, `ROLE_ADMIN`). Proteger também `converter-venda`.

#### [SEC-02] API sem autenticação expõe dados pessoais e operações de escrita, e fica acessível pela rede local
- **Severidade:** `HIGH` (era `MEDIUM` em 02/10/2026)
- **Localização:** todos os controllers (`/carros`, `/clientes`, `/vendas`, `/propostas`, `/dashboard`, fotos e galeria); `docker-compose.yml:35` (`"8080:8080"`) e `:48` (`"80:80"`)
- **Evidência:**
```yaml
# docker-compose.yml — portas publicadas em todas as interfaces (0.0.0.0)
backend:
  ports:
    - "8080:8080"
frontend:
  ports:
    - "80:80"
```
- **Impacto:** `pom.xml` não contém `spring-boot-starter-security`. Qualquer pessoa que alcance a máquina (mesma rede Wi-Fi/LAN) pode:
  - ler **dados pessoais de clientes** (nome, CPF/CNPJ, telefone, e-mail, endereço — relevante para a LGPD);
  - ler custos, margens e faturamento;
  - criar, alterar e excluir veículos, clientes, vendas, fotos e políticas comerciais.

  A severidade subiu em relação à auditoria anterior porque (a) o domínio agora contém PII e dados financeiros sensíveis, (b) o Docker publica as portas em todas as interfaces, não apenas em `localhost`, e (c) a Fase 7 passou a depender de perfis que só fazem sentido com autenticação real.
- **Recomendação:**
  1. **Imediato:** publicar as portas apenas em loopback enquanto não houver autenticação (`"127.0.0.1:80:80"`, `"127.0.0.1:8080:8080"`), ou remover a publicação da 8080, já que o front-end acessa o backend pela rede interna do Docker.
  2. **Definitivo:** adicionar Spring Security com autenticação (sessão ou JWT) e regras por papel, conforme esboço na seção 3.

### B. Integridade de Negócio

#### [SEC-03] Fluxo de alçadas e preço mínimo contornável pela API de vendas
- **Severidade:** `HIGH`
- **Localização:**
  - `src/main/java/com/concessionaria/gomez/api/controller/VendaController.java:47` (`POST /vendas`), `:68` (`PUT /vendas/{id}`), `:93` (`DELETE /vendas/{id}`)
  - `src/main/java/com/concessionaria/gomez/domain/service/EmissaoVendaService.java:28` (`realizarVenda`), `:58` (`atualizar`), `:108` (`cancelar`)
- **Evidência:**
```java
// POST /vendas sem propostaId → emissão direta, valor livre
venda = emissaoVendaService.realizarVenda(carroId, clienteId, vendaInput.getValorVenda(), observacoes);

// EmissaoVendaService.realizarVenda — só verifica se o carro está ativo
if (!Boolean.TRUE.equals(carro.getAtivo())) { throw ... }
// não verifica reserva vigente (RESERVADO), política comercial nem piso de margem

// EmissaoVendaService.atualizar — altera valor de qualquer venda, inclusive convertida de proposta ou já cancelada
venda.setValorVenda(novoValorVenda);

// EmissaoVendaService.cancelar (DELETE) — exclusão física, sem motivo nem responsável
vendaRepository.delete(venda);
```
- **Impacto:**
  - Toda a máquina de estados da Fase 7 (simulação → alçada → aprovação gerencial → conversão com valores congelados) pode ser **ignorada**: basta `POST /vendas` sem `propostaId`, com qualquer valor positivo (por exemplo, R$ 1,00).
  - É possível vender um veículo **reservado** por outra proposta em andamento.
  - `PUT /vendas/{id}` permite reescrever o valor de vendas que deveriam estar congeladas (originadas de proposta) ou canceladas.
  - `DELETE /vendas/{id}` apaga o registro sem trilha, contornando o `cancelamento-auditado`.
  - Mesmo após autenticação (SEC-02), um usuário com perfil VENDEDOR continuaria podendo fazer isso se não houver regra de papel nestas rotas.
- **Recomendação:**
  - Restringir a emissão direta (`POST /vendas` sem proposta) a ADMIN, ou exigir proposta aprovada.
  - Bloquear emissão direta para veículos com reserva vigente.
  - Proibir `PUT` em vendas canceladas ou originadas de proposta (`revisaoConvertida != null`).
  - Descontinuar o `DELETE /vendas/{id}` em favor de `POST /vendas/{id}/cancelamento-auditado`, ou restringi-lo a ADMIN.

#### [SEC-05] `DELETE /vendas/{id}` pode reativar um veículo já revendido (venda dupla)
- **Severidade:** `MEDIUM`
- **Localização:** `src/main/java/com/concessionaria/gomez/domain/service/EmissaoVendaService.java:108-120`
- **Evidência:**
```java
public void cancelar(Long vendaId) {
    Venda venda = buscarOuFalhar(vendaId);
    Carro carro = venda.getCarro();
    if (carro != null) {
        carro.ativar();          // reativa sem verificar se a venda já estava cancelada
        cadastroCarroService.salvar(carro);
    }
    vendaRepository.delete(venda);
```
- **Cenário (derivado da leitura do código; não reproduzido para não alterar dados reais):**
  1. A venda A do carro X é cancelada com auditoria e o carro volta ao estoque.
  2. O carro X é vendido de novo (venda B), ficando `VENDIDO` e inativo.
  3. `DELETE /vendas/A` → `carro.ativar()` torna o carro X `DISPONIVEL` outra vez, embora a venda B esteja vigente.
  4. O carro X pode ser vendido uma terceira vez.
- **Recomendação:** recusar `DELETE` de venda já cancelada e só reativar o veículo se a venda excluída for a venda vigente dele. A solução preferível é a descontinuação do `DELETE`, como proposto em SEC-03.

### C. Vazamento de Dados

#### [SEC-04] Custos e margens ocultos no dashboard continuam expostos por outras rotas
- **Severidade:** `HIGH` 🔁 *(revisado; original `MEDIUM` — justificativa em §8.3)*
- **Localização:**
  - `src/main/java/com/concessionaria/gomez/api/model/CarroModel.java:20` (`compra`)
  - `src/main/java/com/concessionaria/gomez/api/controller/PropostaComercialController.java:180` (`GET /propostas/politica-vigente` retorna a entidade JPA `PoliticaComercial`)
  - 🔁 `src/main/java/com/concessionaria/gomez/infrastructure/repository/spec/CarroSpecs.java:40,44` e `domain/repository/CarroRepository.java:20-27`: os filtros `minValor`/`maxValor` atuam sobre `compra` (identificado pelo Gemini; ver §8.1)
- **Verificado em execução (somente leitura):**
  - `GET /api/carros/1` com `X-Operador-Perfil: VENDEDOR` → contém `"compra":15000.00`.
  - `GET /api/propostas/politica-vigente` com `X-Operador-Perfil: VENDEDOR` → contém `margemAutonomia`, `margemPisoAbsoluto` e limites de desconto.
- **Impacto:** a Fase 7 oculta custos e margens do VENDEDOR no dashboard e nas propostas, mas o custo de cada veículo aparece em `/carros`, e a margem de piso aparece em `politica-vigente`. Com isso, o vendedor sabe exatamente até onde pode negociar, o que anula o objetivo da ocultação. Além disso, `politica-vigente` e `POST /propostas/politicas` devolvem a **entidade JPA** diretamente, sem DTO. Uma política do tipo `EXCECAO_VEICULO` serializaria o objeto `Carro` associado, junto com o custo.
- **Recomendação:** aplicar a mesma regra `podeVerCustosEMargensInternas()` no `CarroModelAssembler` (omitir `compra` para VENDEDOR) e criar um DTO para `PoliticaComercial` com versão restrita para VENDEDOR. Considerar também bloquear a ordenação por `compra` (`?sort=compra`) para esse perfil.

### D. Configuração de Rede, Infraestrutura & Headers

#### [SEC-06] CORS irrestrito (`@CrossOrigin(origins = "*")`) em 6 controllers
- **Severidade:** `MEDIUM` (era `LOW`)
- **Localização:** `CarroController`, `CarroFotoController`, `ClienteController`, `DashboardController`, `PropostaComercialController`, `VendaController`
- **Impacto:** sem autenticação, o `*` permite que **qualquer site aberto no navegador do funcionário** leia as respostas de `http://localhost:8080` (inclusive PII de clientes) e envie requisições de escrita que exigem *preflight* (`PUT`, `DELETE`, JSON). A severidade subiu porque o volume de dados sensíveis aumentou. Hoje o front-end acessa a API pela mesma origem (proxy `/api` no Nginx), então o CORS aberto **não é necessário** para o funcionamento da aplicação em Docker.
- **Recomendação:** remover os `@CrossOrigin` e, se for necessário para o `npm run dev`, configurar uma política global (`WebMvcConfigurer#addCorsMappings` ou `CorsConfigurationSource` no Spring Security) restrita a `http://localhost:5173`.

#### [SEC-07] MySQL exposto em todas as interfaces, com usuário `root` e senha publicada em repositório público
- **Severidade:** `MEDIUM`
- **Localização:** `docker-compose.yml:7`, `:10`, `:14`, `:32`; `.github/workflows/ci.yml:18`; `src/main/resources/application.properties:3` (valor padrão)
- **Evidência:**
```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: <senha-de-desenvolvimento>   # mesma senha em 3 arquivos versionados
  ports:
    - "3307:3306"                                       # publicada em 0.0.0.0
```
- **Impacto:** pelas regras de `refs/secrets.md`, credenciais locais de `docker-compose` seriam `LOW`/`INFO`. Este caso foi **elevado** por três fatores combinados: (1) o repositório `gomez1983/concessionariaGomez` é **público** no GitHub, então a senha é conhecida por qualquer pessoa; (2) a porta 3307 está publicada em todas as interfaces; (3) o usuário é `root`. Juntos, permitem que qualquer pessoa na mesma rede acesse o banco completo como administrador. Se essa senha for reutilizada em outro serviço, o risco se estende a ele.
- **Recomendação:** publicar a porta apenas em loopback (`"127.0.0.1:3307:3306"`). Mover as credenciais para um arquivo `.env` não versionado, com `.env.example` de modelo. No CI, usar `secrets` do GitHub Actions. **Não reutilizar essa senha** em nenhum outro lugar.

#### [SEC-08] Tomcat embarcado 10.1.30 vulnerável a DoS em upload multipart (CVE-2025-48988), e Spring Boot 3.3.x sem suporte
- **Severidade:** `MEDIUM`
- **Localização:** `pom.xml:8` (`spring-boot-starter-parent` 3.3.4 → `tomcat-embed-core` 10.1.30)
- **Evidência:** a [CVE-2025-48988](https://nvd.nist.gov/vuln/detail/CVE-2025-48988) afeta o Tomcat 10.1.0-M1 a 10.1.41 e foi corrigida na 10.1.42 ([Apache Tomcat 10 vulnerabilities](https://tomcat.apache.org/security-10.html)). Uma requisição multipart com grande número de partes pode esgotar a memória. **O projeto aceita multipart** em `PUT /carros/{id}/foto` e `POST/PUT /carros/{id}/fotos`, sem autenticação, então o vetor está exposto.
- **Impacto:** negação de serviço da API por qualquer pessoa que alcance a porta. Além disso, a linha **Spring Boot 3.3.x encerrou o suporte** (OSS em 30/06/2025 e comercial em 30/06/2026), então novas correções não chegarão mais por essa linha.
- **Recomendação:** atualizar para a linha suportada mais recente do Spring Boot 3.5.x, o que traz Tomcat e Spring Framework corrigidos. Até lá, é possível sobrescrever apenas o Tomcat com `<tomcat.version>` no `pom.xml`, como medida temporária.

### E. Tratamento de Erros & Higiene

#### [SEC-09] Mensagens de exceções internas repassadas ao cliente
- **Severidade:** `LOW`
- **Localização:** `infrastructure/service/storage/DiscoLocalFotoStorageService.java:27,37,56`; `api/controller/CarroFotoController.java:57`; `application.properties:13` (`server.error.include-message=ALWAYS`)
- **Evidência:**
```java
throw new NegocioException("Não foi possível armazenar o arquivo da foto: " + e.getMessage());
```
- **Impacto:** a mensagem de `IOException` normalmente contém o **caminho absoluto no servidor** (por exemplo `/app/catalogo-fotos/<uuid>.jpg`) e detalhes do sistema de arquivos, que o `ApiExceptionHandler` devolve no campo `detail`. É reconhecimento útil para um atacante, sem impacto direto.
- **Recomendação:** registrar `e` no log e devolver apenas uma mensagem genérica ao cliente. Remover `include-message=ALWAYS` (ou usar `never`) no perfil de produção.

#### [SEC-10] Nginx sem cabeçalhos de segurança
- **Severidade:** `LOW`
- **Localização:** `frontend/nginx.conf`; `api/controller/CarroFotoController.java:68` (foto principal servida sem `X-Content-Type-Options`, ao contrário da galeria)
- **Impacto:** ausência de `Content-Security-Policy`, `X-Frame-Options`/`frame-ancestors`, `X-Content-Type-Options: nosniff` e `Referrer-Policy`. Isso enfraquece a defesa em profundidade contra XSS e *clickjacking*. Além disso, `server_tokens` está ativo, o que divulga a versão do Nginx.
- **Recomendação:** adicionar ao `server {}`:
  ```nginx
  server_tokens off;
  add_header X-Content-Type-Options "nosniff" always;
  add_header X-Frame-Options "DENY" always;
  add_header Referrer-Policy "strict-origin-when-cross-origin" always;
  add_header Content-Security-Policy "default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'" always;
  ```
  Valide a CSP no navegador antes de manter, porque *inline styles* e as imagens de referência externas podem exigir ajustes. Observação: no Nginx, os `add_header` de um `location` substituem os do `server`, então revise os blocos `location` que já usam `add_header`.

#### [SEC-11] Aplicação conecta ao banco como `root`
- **Severidade:** `LOW`
- **Localização:** `docker-compose.yml:31`; `application.properties:2` (padrão `root`)
- **Impacto:** viola o princípio do menor privilégio. Uma eventual injeção ou abuso de credencial teria permissões totais sobre o servidor MySQL, e não apenas sobre o schema `concessionaria`.
- **Recomendação:** criar um usuário dedicado (`MYSQL_USER`/`MYSQL_PASSWORD` no compose), com permissões restritas ao schema da aplicação.

### F. Observações

#### [SEC-12] Swagger UI exposto pela porta 80
- **Severidade:** `INFO`
- **Localização:** `frontend/nginx.conf` (proxy `/swagger-ui/` e `/v3/api-docs`)
- **Observação:** é adequado para desenvolvimento (ver `refs/false-positives.md` §1.2). Em produção, desativar com `springdoc.swagger-ui.enabled=false` e `springdoc.api-docs.enabled=false`, ou proteger por autenticação.

#### [SEC-13] `spring.jpa.show-sql=true` e `ex.printStackTrace()`
- **Severidade:** `INFO`
- **Localização:** `application.properties:10`; `ApiExceptionHandler.java:85`
- **Observação:** o `show-sql` não mostra valores de parâmetros, mas gera ruído. O `printStackTrace()` escreve no `stderr` fora do Logback, perdendo nível, formato e correlação. Recomenda-se `log.error("...", ex)` e `show-sql` desligado fora do ambiente local.

---

## 🛠️ 3. Propostas de Correção (Patches Sugeridos)

> ⚠️ **Revise cada patch antes de aplicar. Nada foi alterado no repositório.**

### SEC-01 — Mitigação imediata: padrão de menor privilégio

```diff
--- a/src/main/java/com/concessionaria/gomez/domain/model/PerfilOperadorComercial.java
+++ b/src/main/java/com/concessionaria/gomez/domain/model/PerfilOperadorComercial.java
@@ -19,8 +19,8 @@
 
     public static PerfilOperadorComercial fromHeader(String raw) {
         if (raw == null || raw.isBlank()) {
-            return GERENTE;
+            return VENDEDOR; // fail-closed: sem identificação, menor privilégio
         }
```
> Impacto colateral: chamadas do Postman ou de testes que hoje omitem o cabeçalho e esperam a visão gerencial passarão a receber a visão de VENDEDOR. Ajuste a coleção `concessionaria-postman-collection.json` e os testes que dependam disso. O front-end não é afetado, porque sempre envia o cabeçalho.

### SEC-02 / SEC-01 definitivo — Esboço de Spring Security (decisão de arquitetura)

```java
// Esboço ilustrativo — o mecanismo (sessão, JWT, OAuth2) é decisão do time.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // apenas se a autenticação for por token (stateless)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/propostas/*/aprovacoes", "/propostas/politicas")
                    .hasAnyRole("GERENTE", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/propostas/*/converter-venda")
                    .hasAnyRole("GERENTE", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/vendas/**", "/carros/**", "/clientes/**")
                    .hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            .build();
    }
}
```
Depois disso, substitua `@RequestHeader("X-Operador-Perfil")`/`("X-Operador-Id")` por dados do `Authentication` e remova o envio desses cabeçalhos em `frontend/src/lib/api.ts`.

### SEC-02 / SEC-07 — Publicar portas apenas em loopback (até haver autenticação)

```diff
--- a/docker-compose.yml
+++ b/docker-compose.yml
@@ mysql
     ports:
-      - "3307:3306"
+      - "127.0.0.1:3307:3306"
@@ backend
     ports:
-      - "8080:8080"
+      - "127.0.0.1:8080:8080"
@@ frontend
     ports:
-      - "80:80"
+      - "127.0.0.1:80:80"
```

### SEC-03 / SEC-05 — Proteger o ciclo de vida das vendas

```diff
--- a/src/main/java/com/concessionaria/gomez/domain/service/EmissaoVendaService.java
+++ b/src/main/java/com/concessionaria/gomez/domain/service/EmissaoVendaService.java
@@ public Venda realizarVenda(Long carroId, Long clienteId, BigDecimal valorVenda, String observacoes) {
         if (!Boolean.TRUE.equals(carro.getAtivo())) {
             throw new NegocioException(...);
         }
+        if (carro.getDisponibilidadeComercial() == DisponibilidadeComercial.RESERVADO
+                && carro.getReservaValidaAte() != null
+                && carro.getReservaValidaAte().isAfter(OffsetDateTime.now())) {
+            throw new ConflitoNegociacaoException(
+                    "O veículo está reservado por uma proposta comercial em andamento.");
+        }
@@ public Venda atualizar(Long vendaId, Long novoCarroId, Long novoClienteId, BigDecimal novoValorVenda, String novasObservacoes) {
         Venda venda = buscarOuFalhar(vendaId);
+        if (Boolean.TRUE.equals(venda.getCancelada()) || venda.getRevisaoConvertida() != null) {
+            throw new NegocioException(
+                    "Vendas canceladas ou originadas de proposta aprovada não podem ser alteradas.");
+        }
@@ public void cancelar(Long vendaId) {
         Venda venda = buscarOuFalhar(vendaId);
+        if (Boolean.TRUE.equals(venda.getCancelada())) {
+            throw new NegocioException(
+                    "Esta venda já foi cancelada com auditoria e não pode ser excluída.");
+        }
```
> Requer os imports de `DisponibilidadeComercial` e `ConflitoNegociacaoException`. A restrição de **quem** pode emitir venda direta, alterar ou excluir depende de autenticação (SEC-02). Verifique os testes `EmissaoVendaServiceTest` e `CadastroCarroIT` que exercitam essas rotas.

---

## 📦 4. Análise de Dependências e CVEs

| Componente | Versão | Advisory | Aplica-se ao projeto? |
| :--- | :--- | :--- | :--- |
| `tomcat-embed-core` | 10.1.30 | [CVE-2025-48988](https://nvd.nist.gov/vuln/detail/CVE-2025-48988) — DoS em upload multipart (corrigido em 10.1.42) | **Sim** — há upload multipart sem autenticação → **SEC-08** |
| `spring-webmvc` | 6.1.13 | [CVE-2024-38819](https://spring.io/security/cve-2024-38819/) — path traversal em functional web frameworks (corrigido em 6.1.14) | **Não** — o projeto não usa `RouterFunction`/WebMvc.fn |
| `spring-context` | 6.1.13 | [CVE-2024-38820](https://spring.io/security/cve-2024-38820/) e [CVE-2025-22233](https://spring.io/security/cve-2025-22233/) — `DataBinder.disallowedFields` | **Não** — o projeto não usa `setDisallowedFields`/`@InitBinder` |
| Spring Boot | 3.3.4 | Linha 3.3.x sem suporte (OSS até 30/06/2025, comercial até 30/06/2026) | Sim — sem novas correções → incluído em **SEC-08** |

- **Frontend:** `npm audit --omit=dev` → **0 vulnerabilidades**.
- **Limitação:** a varredura Maven foi manual (versões resolvidas via `mvn dependency:list` e advisories oficiais), sem scanner automatizado. Para cobertura completa, recomenda-se adicionar o OWASP Dependency-Check ou o Dependabot ao CI.

Fontes: [Spring — CVE-2024-38819 e CVE-2024-38820](https://spring.io/blog/2024/10/17/spring-framework-cve-2024-38819-and-cve-2024-38820-published/), [Apache Tomcat 10 vulnerabilities](https://tomcat.apache.org/security-10.html), [Anúncio CVE-2025-48988 (lists.apache.org)](https://lists.apache.org/thread/nzkqsok8t42qofgqfmck536mtyzygp18).

---

## 🔑 5. Varredura de Segredos e Arquivos Rastreados
- **Arquivos sensíveis rastreados no Git:** nenhum (`.env`, chaves, keystores ou `application-prod.*` não estão versionados). `frontend/.env.example` contém apenas URLs públicas. `catalogo-fotos/` está corretamente no `.gitignore` e no `.dockerignore`.
- **Credenciais em arquivos de configuração:** a senha do MySQL de desenvolvimento aparece em `docker-compose.yml` (3 ocorrências), `.github/workflows/ci.yml` e como valor padrão em `application.properties`. Como o repositório é público → **SEC-07**. O valor não é reproduzido neste relatório.

---

## 🧹 6. Falsos Positivos Descartados
- **SQL/JPQL Injection:** as consultas `@Query` em `CarroRepository`, `ClienteRepository`, `PoliticaComercialRepository` e `VendaRepository` usam apenas parâmetros nomeados (`:ano`, `:minValor`…); a concatenação é entre literais. `CarroSpecs` usa Criteria API. Sem risco.
- **Mass Assignment:** `CarroInput`, `ClienteInput` e `VendaInput` não expõem `id`, `ativo`, datas de sistema nem status. Todos os `@RequestBody` têm `@Valid`. O Jackson rejeita propriedades desconhecidas (`fail-on-unknown-properties=true`). 🔁 *Correção (§8.1): este descarte não se estende ao `PropostaComercialInput`, cuja semântica não havia sido analisada; o campo `itensExtras` permite o abuso descrito em SEC-14.*
- **Upload de imagens:** `ImagemValidator` confere os *magic bytes* contra o `Content-Type` declarado (JPEG/PNG/WebP), limita o tamanho (10 MB) e as dimensões (≤ 12000 px e ≤ 40 MP, mitigando *decompression bombs*). Os nomes de arquivo são UUIDs gerados no servidor, e `getArquivoPath` impede *path traversal*.
- **CVE-2024-38819 / CVE-2024-38820 / CVE-2025-22233:** presentes na versão do Spring Framework, mas não exploráveis neste código (ver seção 4).
- **XXE via `jackson-dataformat-xml`:** o Jackson 2.17 com Woodstox 6.7.0 não resolve entidades externas por padrão.
- **XSS no front-end:** não há `dangerouslySetInnerHTML`, `innerHTML` nem `eval`. Os `href` dinâmicos em `VehicleGallery.tsx` vêm de `src/data/referencePhotos.json`, estático e empacotado no build, e não de entrada do usuário. O `localStorage` guarda apenas a preferência de tema.
- **Tokens no navegador:** não se aplica (não há autenticação).
- **Actuator:** não presente no `pom.xml`.
- **DevTools em produção:** o `spring-boot-devtools` é opcional e não é empacotado no JAR do container. O log do container mostra a thread `main`, e não `restartedMain`.
- **Container do backend:** roda com usuário sem privilégios (`appuser`), em imagem JRE Alpine. Boa prática confirmada.
- **Dados de seed (`afterMigrate.sql`):** ficam em `src/test/resources` e são aplicados apenas no banco de teste (`concessionaria_test`). Descartado (`refs/false-positives.md` §1.3).

---

## 🎯 7. Conclusão e Próximos Passos

O código tem boa higiene nas camadas clássicas: não há SQL injection nem mass assignment, o upload de imagens é bem validado e o container do backend roda sem `root`. O risco concentra-se em **controle de acesso**. A Fase 7 introduziu regras de alçada, perfis e auditoria que **dependem de identidade**, mas a identidade é declarada pelo próprio cliente, assume `GERENTE` por omissão e pode ser contornada por outras rotas (`/vendas`, `/carros`, `politica-vigente`).

**Ordem sugerida:**
1. **Hoje, sem mudar arquitetura:**
   - Publicar as portas do Docker apenas em `127.0.0.1` (SEC-02/SEC-07).
   - Inverter o padrão de `fromHeader` para `VENDEDOR` (SEC-01).
   - Remover os `@CrossOrigin("*")` (SEC-06).
2. **Atualizar o Spring Boot para 3.5.x** (SEC-08). Deve vir antes do Spring Security, para não construir sobre uma linha sem suporte.
3. **Implementar Spring Security** com papéis, removendo os cabeçalhos `X-Operador-*` (SEC-01/SEC-02), e proteger o ciclo de vida das vendas (SEC-03/SEC-05).
4. **Ajustes de exposição e higiene:**
   - Aplicar o DTO e a ocultação de custos (SEC-04).
   - Usar usuário de banco dedicado e mover as credenciais para `.env` (SEC-07/SEC-11).
   - Adicionar os cabeçalhos do Nginx (SEC-10).
   - Usar mensagens de erro genéricas (SEC-09).

> 🔁 **Ajustes na ordem acima após a revisão cruzada:** ver §8.7.

**Método e limitações:** análise estática do código no commit `bbb30fe`, mais requisições **somente leitura** (`GET`) contra o ambiente Docker local para confirmar SEC-01 e SEC-04. Nenhuma requisição de escrita foi executada, e nenhum dado foi alterado. O cenário de SEC-05 foi derivado da leitura do código e não reproduzido. A análise de dependências Maven foi manual. Os achados da seção 8 vieram da revisão cruzada com o relatório do Gemini e foram verificados no código; SEC-15 não foi reproduzido.

---

## 🔁 8. Adendo — Revisão Cruzada com a Auditoria do Gemini

O Gemini executou a mesma skill `security-audit` em paralelo ([`2026-11-25-auditoria.md`](2026-11-25-auditoria.md)). Cada achado que aparecia apenas no relatório dele foi verificado diretamente no código antes de ser incorporado aqui. Os achados originais deste relatório (SEC-01 a SEC-13) foram mantidos como registro histórico, com marcações 🔁 onde houve revisão.

**Convergência:** os dois relatórios concordam no núcleo:
- perfil por cabeçalho com padrão `GERENTE`;
- ausência de autenticação;
- custos e margens expostos para o VENDEDOR;
- contorno das alçadas via `POST /vendas`;
- CORS `*`, mensagens de erro com caminhos internos, cabeçalhos ausentes no Nginx, Swagger e `show-sql`;
- os mesmos controles positivos (sem SQLi, upload bem validado, front-end sem XSS).

### 8.1 Achados do Gemini confirmados no código

#### [SEC-14] Vendedor reduz o piso de margem declarando "receita adicional" na proposta
- **Severidade:** `HIGH` *(o Gemini classificou como `MEDIUM`)*
- **Origem:** Gemini (SEC-03 do relatório dele) — **confirmado**.
- **Localização:**
  - `src/main/java/com/concessionaria/gomez/api/model/input/PropostaComercialInput.java:52-62` (`ItemFinanceiroInput`: `confirmado = true` por padrão, `valor` sem teto)
  - `src/main/java/com/concessionaria/gomez/api/assembler/PropostaComercialAssembler.java:38` (copia `confirmado` do payload)
  - `src/main/java/com/concessionaria/gomez/domain/service/MotorFinanceiroComercialService.java:166-167` (soma o item em `B`)
- **Evidência:**
```java
// PropostaComercialInput.ItemFinanceiroInput
private BaseCalculoItemFinanceiro baseCalculo = BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS;
@PositiveOrZero
private BigDecimal valor;            // sem limite superior
private boolean confirmado = true;   // confirmado por padrão, definido pelo cliente

// MotorFinanceiroComercialService
} else if (item.getBaseCalculo() == BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL) {
    b = b.add(valorItem.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP));
}
```
- **Impacto:** o piso é `(C + F − B) / (1 − t − m)`. Um vendedor que envia em `POST /propostas` (ou em uma nova revisão) um item `RECEITA_ADICIONAL_ELEGIVEL` com valor ≥ `C + F` leva o piso a zero ou abaixo. Com isso, **qualquer preço** recebe aprovação automática (`APROVADA_POLITICA_AUTOMATICA`), sem passar pelo gerente. O motivo de ser `HIGH`: o abuso usa o **perfil legítimo** de VENDEDOR. A implementação do Spring Security (SEC-02) **não resolve** este item sozinha.
- **Recomendação:** receita adicional declarada no payload só deve contar como confirmada quando enviada por quem tem alçada gerencial. Vinda do vendedor, deve entrar como **pendente**, o que já leva a proposta ao estado `PENDENTE_DADOS_OBRIGATORIOS` e bloqueia a aprovação até a confirmação. Opcionalmente:
  - limitar o valor a uma fração do preço negociado;
  - aceitar receitas elegíveis apenas por campanhas cadastradas (`CampanhaComercial`), e não por texto livre.
- **Patch sugerido** (esboço; ver nota):
```diff
--- a/src/main/java/com/concessionaria/gomez/api/assembler/PropostaComercialAssembler.java
+++ b/src/main/java/com/concessionaria/gomez/api/assembler/PropostaComercialAssembler.java
@@ public GestaoPropostaComercialService.DadosRevisaoCommand toCommand(
             PropostaComercialInput input,
-            String vendedorIdentificador) {
+            String vendedorIdentificador,
+            PerfilOperadorComercial perfil) {
+
+        boolean podeConfirmarReceita = perfil != null && perfil.podeAprovarExcecaoGerencial();
 
         List<ItemFinanceiroRevisao> itens = input.getItensExtras() == null
                 ? Collections.emptyList()
                 : input.getItensExtras().stream().map(i -> {
                     ItemFinanceiroRevisao item = new ItemFinanceiroRevisao();
                     ...
-                    item.setConfirmado(i.isConfirmado());
+                    boolean receitaDeclarada =
+                            i.getBaseCalculo() == BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL;
+                    // Receita adicional declarada pelo vendedor entra pendente e exige confirmação gerencial.
+                    item.setConfirmado(i.isConfirmado() && (!receitaDeclarada || podeConfirmarReceita));
```
> Nota: as três chamadas de `toCommand` no `PropostaComercialController` (simulação, criação e revisão) precisam passar o `perfil`, que já está disponível nesses métodos. **Não existe endpoint dedicado para confirmar itens**: a confirmação acontece por uma nova revisão enviada por GERENTE/ADMIN. Enquanto o perfil vier de cabeçalho (SEC-01), a proteção vale para o uso normal pela interface, mas continua forjável. Ela só fica completa com autenticação real.

#### Filtros `minValor`/`maxValor` como oráculo do custo → incorporado em SEC-04
- **Origem:** Gemini — **confirmado**. Os filtros atuam sobre `compra` em `CarroSpecs.java:40,44` e também na consulta `findComFiltros` (`CarroRepository.java:20-27`).
- **Impacto:** mesmo depois de omitir `compra` no JSON e bloquear `?sort=compra`, um vendedor descobre o custo exato de qualquer veículo em poucas chamadas, por busca binária (`?minValor=50000&maxValor=55000`…).
- **Recomendação:** para perfis sem `podeVerCustosEMargensInternas()`, aplicar os filtros sobre o preço anunciado ou sugerido, ou ignorá-los.

#### [SEC-15] Entidade `PoliticaComercial` serializada diretamente, com possível erro 500
- **Severidade:** `MEDIUM`
- **Origem:** Gemini — **plausível, não reproduzido** (não há política `EXCECAO_VEICULO` cadastrada para testar).
- **Localização:** `PropostaComercialController.java:180` e `:194`; `domain/model/PoliticaComercial.java:27-29` (`@ManyToOne(fetch = LAZY) Carro carro`); `PoliticaComercialRepository.java:16-24` (consulta sem `join fetch`).
- **Correção técnica da explicação do Gemini:** o relatório dele atribui o erro a acesso "fora de sessão". Porém o `spring.jpa.open-in-view` está no padrão (`true`), então a sessão continua aberta durante a serialização. O erro provável vem do Jackson, que não tem o módulo do Hibernate (`jackson-datatype-hibernate`) e não sabe serializar os campos internos do proxy (`hibernateLazyInitializer`). Isso gera `InvalidDefinitionException`, convertida em 500 pelo `ApiExceptionHandler`. Se a serialização tivesse sucesso, o `Carro` sairia completo, **com o custo**.
- **Recomendação:** mesma correção de SEC-04, com um DTO `PoliticaComercialModel` contendo apenas o `carroId` e com versão restrita para VENDEDOR.

#### [SEC-16] Container do Nginx executando como `root`
- **Severidade:** `LOW`
- **Origem:** Gemini — **confirmado** (`frontend/Dockerfile:18`, `FROM nginx:alpine`).
- **Impacto:** o processo mestre do Nginx roda como `root` dentro do container; os *workers* rodam como `nginx`. Se houver uma falha no Nginx, o impacto dentro do container é maior. O container do backend, por comparação, já roda como `appuser`.
- **Recomendação:** usar `nginxinc/nginx-unprivileged:alpine`. Essa imagem escuta na porta 8080, então é preciso ajustar o `listen` do `nginx.conf` e o mapeamento no compose (`"127.0.0.1:80:8080"`).

### 8.2 Evidência nova encontrada durante a revisão cruzada

O Gemini registrou o parâmetro `operadorOverrideId` em `frontend/src/lib/api.ts:191-201`, mas não descreveu o impacto. Ao seguir o uso desse parâmetro:

```tsx
// frontend/src/components/Proposals.tsx:120-131
const decisorId =
  ctx.operadorId.toLowerCase() === prop.vendedorIdentificador?.toLowerCase()
    ? 'diretoria.aprovacao@concessionariagomez.com.br'
    : ctx.operadorId;

await api.decidirAprovacaoProposta(prop.id, rev.sequencia, aprovar, motivo, decisorId);
```

- **Impacto:** quando o operador logado na tela é o vendedor da proposta, **o próprio front-end substitui a identidade** por um e-mail fictício de "diretoria". A checagem de conflito de interesse do backend (`GestaoPropostaComercialService.java:160`) nunca é acionada. A trilha de auditoria registra um aprovador que não existe. Isso acontece no **uso normal** da aplicação, sem nenhuma manipulação, e é a evidência mais forte de SEC-01.
- **Recomendação:** remover essa substituição do front-end. A autoaprovação deve ser recusada pelo backend, e o decisor deve vir do usuário autenticado (SEC-02), nunca de um parâmetro do cliente.

### 8.3 Reclassificações

| ID | Antes | Depois | Justificativa |
| :--- | :---: | :---: | :--- |
| SEC-04 | MEDIUM | **HIGH** | (1) O front-end já exibe "Restrito" quando `compra` vem nulo (`frontend/src/components/Stock.tsx:384, 450, 880`), o que prova que a ocultação era um **controle pretendido e quebrado**, não uma melhoria opcional. (2) Os filtros `minValor`/`maxValor` permitem descobrir o custo mesmo após ocultar o campo. (3) Com `margemPisoAbsoluto` (via `politica-vigente`), o vendedor reconstrói o piso exato da loja. |
| SEC-01 | HIGH | HIGH | Severidade mantida; evidência reforçada (§8.2). |

### 8.4 Divergências e erros identificados no relatório do Gemini

1. **Dependências:** o relatório afirma "nenhuma CVE crítica conhecida afetando os fluxos utilizados". A CVE-2025-48988 do Tomcat 10.1.30 afeta diretamente o upload multipart de fotos (SEC-08), e o relatório também não menciona que a linha Spring Boot 3.3.x está sem suporte.
2. **Senha em texto claro:** a senha do MySQL de desenvolvimento aparece duas vezes no relatório (seções SEC-07 e 5). Como o repositório é **público**, ela deve ser mascarada antes do commit.
3. **Patch 2 incompleto (`CarroModelAssembler`):** o `toModel(carro)` passa a usar `VENDEDOR` como padrão. Aplicado sozinho, esconde `compra` **também de GERENTE e ADMIN** até que todos os chamadores passem o perfil. O estoque mostraria "Restrito" para gerentes, e a edição de veículos pode falhar, porque `CarroInput.compra` é `@NotNull`. Além disso, não trata o oráculo dos filtros. O patch precisa vir junto das alterações nos controllers e em `CarroSpecs`/`CarroRepository`.
4. **Severidade do contorno de alçadas:** o Gemini classificou o contorno via `/vendas` e a receita adicional como `MEDIUM`. Aqui ambos são `HIGH` (SEC-03 e SEC-14), porque permitem perda financeira direta (venda abaixo do piso).
5. **Credenciais:** o Gemini classificou como `LOW`, tratando como ambiente local, sem considerar o repositório público nem a porta 3307 em `0.0.0.0` com usuário `root`. Aqui o item é `MEDIUM` (SEC-07).
6. **Causa do SEC-15:** "fora de sessão" é impreciso (ver §8.1).

### 8.5 Itens cobertos apenas por este relatório (para a consolidação)
- **SEC-01 (partes):**
  - trilha de auditoria falsificável (`decisorAutenticado` e `responsavelCancelamento` aceitam o cabeçalho ou caem em um e-mail fixo de gerente);
  - `converter-venda` sem checagem de perfil.
- **SEC-02 (parte):** as portas 80 e 8080 estão publicadas em todas as interfaces (o Gemini citou apenas a 3307).
- **SEC-05:** venda dupla via `DELETE /vendas/{id}` após cancelamento auditado.
- **SEC-07 (parte):** repositório público como agravante da senha versionada.
- **SEC-08:** CVE-2025-48988 aplicável e Spring Boot 3.3.x sem suporte.
- **SEC-11:** aplicação conecta ao banco como `root`.

### 8.6 Itens cobertos apenas pelo relatório do Gemini (já incorporados acima)
- **SEC-14:** receita adicional declarada pelo vendedor.
- **SEC-04 (parte):** filtros `minValor`/`maxValor` como oráculo do custo.
- **SEC-15:** entidade `PoliticaComercial` com possível erro 500.
- **SEC-16:** container Nginx como `root`.
- **Referências CWE** por achado. São úteis para o relatório consolidado.
- **Inconsistência de CORS** entre `/foto` e `/fotos`. É irrelevante, porque some com a remoção do `@CrossOrigin("*")` (SEC-06).

### 8.7 Impacto na ordem sugerida (seção 7)
- **Etapa 1 (hoje, sem mudar arquitetura):**
  - aplicar SEC-14 (receita declarada pelo vendedor entra pendente);
  - remover a troca de identidade em `Proposals.tsx` (§8.2).
  Nenhum dos dois depende do Spring Security.
- **Etapa 4 (exposição):** a correção de SEC-04 deve incluir os filtros `minValor`/`maxValor` e a ordenação por `compra`. SEC-15 é resolvido junto, com o DTO de `PoliticaComercial`.
- **Hardening do Nginx:** SEC-16 entra junto com SEC-10.
