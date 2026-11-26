# Etapa 3 (Fase 8) — Proposta de Autenticação, Autorização e Login

> **Status:** proposta para aprovação. Nenhum código foi alterado.
> **Base:** Spring Boot 3.5.16 (Spring Security 6.5). A migração para o Boot 4.x continua na Fase 9.
> **Fecha:** SEC-01 e SEC-02 (definitivos), SEC-03 (restrição por papel) e SEC-06 (CORS global).

---

## 1. Visão geral

| Hoje | Depois da Etapa 3 |
|---|---|
| Perfil e identidade chegam pelos cabeçalhos `X-Operador-Perfil` e `X-Operador-Id`, que o cliente pode forjar | Perfil e identidade saem de um **token JWT assinado** pela própria API |
| Qualquer chamada na porta 8080/80 é aceita | Toda rota exige autenticação, exceto login, renovação de sessão, Swagger e `/error` |
| Regras de papel espalhadas (`fromHeader` em cada controller) | **Matriz central** no `SecurityFilterChain` + regras de domínio que já existem (custos, conflito de interesse) |
| Seletor manual de perfil no cabeçalho do painel | Tela de login, usuário logado no cabeçalho e botão "Sair" |
| `@CrossOrigin("*")` em 6 controllers | Um único `CorsConfigurationSource`, mesma origem por padrão |

Fluxo resumido:

```
Navegador                         Nginx (/api)                 API
   │  POST /api/auth/login  ───────────────►  POST /auth/login
   │  ◄── 200 { accessToken, usuario }  + Set-Cookie: refresh (HttpOnly, Path=/api/auth)
   │
   │  GET /api/carros  (Authorization: Bearer <access>) ──► valida assinatura/expiração → perfil
   │
   │  (access expirou → 401)
   │  POST /api/auth/refresh (cookie) ──► rotaciona refresh → novo access
   │  repete a requisição original
   │
   │  POST /api/auth/logout (cookie) ──► revoga a sessão de refresh, apaga o cookie
```

---

## 2. Usuários e migration `V011`

### 2.1 Esquema

```sql
-- V011__cria-usuarios-e-sessoes.sql
create table usuario (
    id bigint not null auto_increment,
    nome varchar(100) not null,
    email varchar(100) not null,             -- mesmo tamanho de vendedor_identificador / decisor_autenticado
    senha_hash varchar(100) not null,        -- formato {bcrypt}$2a$12$... (≈68 caracteres)
    perfil varchar(20) not null,             -- VENDEDOR | GERENTE | ADMIN
    ativo tinyint(1) not null default 1,
    tentativas_falhas int not null default 0,
    bloqueado_ate datetime null,
    ultimo_login datetime null,
    data_cadastro datetime not null,
    data_atualizacao datetime not null,
    primary key (id),
    unique key uk_usuario_email (email)
) engine=InnoDB;

create table sessao_usuario (
    id bigint not null auto_increment,
    usuario_id bigint not null,
    token_hash char(64) not null,            -- SHA-256 do refresh token; o valor puro nunca é gravado
    criada_em datetime not null,
    expira_em datetime not null,             -- limite absoluto da sessão (não estende na rotação)
    revogada_em datetime null,
    primary key (id),
    unique key uk_sessao_token (token_hash),
    constraint fk_sessao_usuario foreign key (usuario_id) references usuario (id)
) engine=InnoDB;
```

- **Só cria tabelas.** Não altera nem apaga nenhuma tabela existente. O volume `mysql_data` é preservado; o Flyway aplica a `V011` na próxima subida, como fez com a `V010`.
- Os campos de auditoria que já existem (`vendedor_identificador`, `decisor_autenticado`, `responsavel_cadastro`, `responsavel_cancelamento`, `reserva_responsavel`) **continuam texto, sem FK** para `usuario`. Eles passam a receber o e-mail do token. Assim o histórico antigo continua válido, sem migração de dados.

### 2.2 Hash de senha

- `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (prefixo `{bcrypt}`, custo 12). O prefixo permite trocar de algoritmo no futuro (ex.: Argon2) sem invalidar senhas antigas.
- Política: **mínimo de 10 e máximo de 72 caracteres** (o BCrypt ignora o que passa de 72 bytes). Sem regras de "símbolo obrigatório" (recomendação NIST SP 800-63B).

### 2.3 Carga inicial sem senha no repositório

O repositório é público, então **nenhuma senha vai para migration nem para código**.

- Um `ApplicationRunner` (`CargaInicialUsuarios`) lê variáveis do `.env` e cria cada usuário **somente se o e-mail ainda não existir**. Nunca altera usuário existente nem redefine senha.
- Usuários iniciais com os **mesmos e-mails já gravados no histórico** das propostas e vendas, para o histórico continuar apontando para pessoas reais:

| Variável no `.env` | E-mail | Perfil |
|---|---|---|
| `SEED_ADMIN_PASSWORD` | `admin@concessionariagomez.com.br` | ADMIN |
| `SEED_GERENTE_PASSWORD` | `gerente@concessionariagomez.com.br` | GERENTE |
| `SEED_VENDEDOR_PASSWORD` | `vendedor@concessionariagomez.com.br` | VENDEDOR |

- Variável vazia ou ausente → o usuário correspondente não é criado. O `.env.example` recebe os placeholders; `docker-compose.yml` repassa as variáveis ao backend.

---

## 3. Token JWT

### 3.1 Biblioteca: `spring-boot-starter-oauth2-resource-server` (recomendado) vs. `jjwt`

| Critério | Resource Server (Nimbus) | `jjwt` |
|---|---|---|
| Versão | Gerenciada pelo BOM do Boot | Dependência extra, versionada à mão |
| Validação do token | Pronta: `BearerTokenAuthenticationFilter`, `JwtDecoder` (assinatura, `exp`, `nbf`, tolerância de relógio) | Filtro próprio a escrever e manter |
| Emissão | `NimbusJwtEncoder` (mesmo pacote) | API própria da biblioteca |
| Erros 401 padronizados | `WWW-Authenticate: Bearer error="invalid_token"` | Implementação manual |
| Boot 4 / Spring Security 7 (Fase 9) | Caminho oficial, migração documentada | Independente, mas o filtro próprio precisa ser revisado |

**Recomendação: Resource Server.** Menos código nosso no caminho crítico de segurança e nenhuma versão extra para acompanhar.

### 3.2 Formato e assinatura

- **HS256** com segredo simétrico `JWT_SECRET` (mínimo 256 bits, Base64) vindo do `.env`. Quem emite e quem valida é a mesma aplicação, então não há ganho em RS256 agora. Se um dia houver outro serviço validando tokens, migramos para RS256/JWKS.
- **Fail-fast:** a aplicação **não sobe** se `JWT_SECRET` estiver ausente ou curto. Não existe valor padrão no `application.properties`.
- Claims: `iss=concessionaria-gomez`, `sub=<e-mail>`, `nome`, `perfil`, `iat`, `exp`, `jti`.
- O `JwtAuthenticationConverter` transforma `perfil` em `ROLE_VENDEDOR` / `ROLE_GERENTE` / `ROLE_ADMIN`.

### 3.3 Tempo de vida e onde guardar na SPA

| Peça | Duração | Onde fica |
|---|---|---|
| Access token (JWT) | **15 minutos** | **Somente em memória** (variável do módulo `api.ts`), nunca em `localStorage`/`sessionStorage` |
| Refresh token (opaco, 256 bits aleatórios) | **10 horas absolutas** (um turno) | Cookie `HttpOnly`, `SameSite=Strict`, `Path=/api/auth`, `Secure` configurável; no banco só o SHA-256 |

- **Rotação:** cada `POST /auth/refresh` revoga o refresh usado e emite outro, mantendo o mesmo limite absoluto da sessão. Reuso de um refresh já revogado revoga **todas** as sessões daquele usuário (indício de roubo).
- **Recarregar a página** não desloga: a SPA chama `/auth/refresh` ao iniciar e recupera o access token pelo cookie.
- **Logout:** revoga a sessão no banco e apaga o cookie. O access token já emitido continua válido até expirar (no máximo 15 min). Trade-off aceito e documentado.
- **Usuário desativado:** não consegue renovar a sessão; perde o acesso em até 15 min.
- **`Secure`:** `app.auth.cookie-secure=true` por padrão. O Chrome e o Firefox aceitam cookie `Secure` em `http://localhost` e `http://127.0.0.1`, então o ambiente local continua funcionando. A propriedade existe para casos fora disso.
- **CSRF:** a API autentica por cabeçalho `Authorization`, que o navegador não envia sozinho, então o CSRF do Spring fica desligado para a API. As únicas rotas que usam cookie são `/auth/refresh` e `/auth/logout`. Elas ficam protegidas por `SameSite=Strict` **e** pela exigência do cabeçalho `X-Requested-With: gomez-spa`, que um formulário de outro site não consegue enviar.
- **Alternativa mais simples (não recomendada):** access token de 8 h em `sessionStorage`, sem refresh. Menos código, porém o token fica legível por qualquer XSS e o logout e a desativação só valem quando o token expira.

### 3.4 Endpoints de autenticação

| Rota (na API) | Rota no navegador | Acesso | Função |
|---|---|---|---|
| `POST /auth/login` | `/api/auth/login` | público | `{email, senha}` → `{accessToken, expiraEm, usuario: {nome, email, perfil}}` + cookie de refresh |
| `POST /auth/refresh` | `/api/auth/refresh` | cookie + `X-Requested-With` | Rotaciona a sessão e devolve um novo access token |
| `POST /auth/logout` | `/api/auth/logout` | cookie + `X-Requested-With` | Revoga a sessão e apaga o cookie (sempre 204) |
| `GET /auth/me` | `/api/auth/me` | autenticado | Dados do usuário logado |
| `PUT /auth/senha` | `/api/auth/senha` | autenticado | Troca a própria senha (exige a atual); revoga as outras sessões |

**Proteção contra força bruta:**
- Mensagem única para e-mail inexistente, senha errada, usuário inativo ou bloqueado: *"E-mail ou senha inválidos."*
- O hash é verificado mesmo quando o e-mail não existe (contra um hash fixo), para o tempo de resposta não revelar quais e-mails existem.
- 5 falhas seguidas → `bloqueado_ate = agora + 15 min`. Login certo zera o contador.

---

## 4. Como o código passa a saber quem é o operador

- Novo `record OperadorAutenticado(String email, String nome, PerfilOperadorComercial perfil)`, resolvido a partir do JWT por um `HandlerMethodArgumentResolver`.
- Nos controllers, **troca mecânica**:
  `@RequestHeader("X-Operador-Perfil") String perfilHeader` + `@RequestHeader("X-Operador-Id") String operadorId` → `OperadorAutenticado operador`.
- Assemblers e serviços **mantêm as assinaturas atuais**: continuam recebendo `PerfilOperadorComercial` e o identificador (`operador.perfil()`, `operador.email()`). A ocultação de custos, o conflito de interesse e a alçada não mudam.
- `PerfilOperadorComercial.fromHeader` e os cabeçalhos `X-Operador-*` **deixam de existir**. Se um cliente ainda os enviar, eles são ignorados.
- Fallbacks de identidade como `"gerente@concessionariagomez.com.br"` em `EmissaoVendaService.cancelarComAuditoria` e na política comercial são removidos: o responsável é sempre o usuário do token.
- Respostas **401 e 403 no mesmo formato Problem** do `ApiExceptionHandler` (`AuthenticationEntryPoint` e `AccessDeniedHandler` próprios), para o frontend exibir `userMessage` como já faz hoje.

---

## 5. Matriz de permissões

Legenda: ✅ permitido · ❌ 403 · **própria** = só propostas em que `vendedorIdentificador` é o e-mail do token (GERENTE/ADMIN operam qualquer uma). Sem token: 401 em todas as linhas.

A matriz fica em **um único `SecurityFilterChain`** (fácil de revisar). As regras que dependem de dados (proposta própria, custos, conflito de interesse) ficam no domínio, como hoje.

### Carros e fotos

| Rota | Método | VENDEDOR | GERENTE | ADMIN | Observação |
|---|---|:-:|:-:|:-:|---|
| `/carros`, `/carros/{id}` | GET | ✅ | ✅ | ✅ | `compra` nulo e filtros por custo 403 para VENDEDOR (Etapa 2) |
| `/carros` | POST | ❌ | ✅ | ✅ | Cadastro exige o custo, que o VENDEDOR não vê |
| `/carros/{id}` | PUT | ❌ | ✅ | ✅ | Idem |
| `/carros/{id}/ativo` | PUT, DELETE | ❌ | ✅ | ✅ | |
| `/carros/{id}` | DELETE | ❌ | ❌ | ✅ | Exclusão física (ver decisão D4) |
| `/carros/{id}/foto`, `/carros/{id}/fotos`, `/fotos/{fotoId}/conteudo` | GET | ✅ | ✅ | ✅ | |
| `/carros/{id}/foto` | PUT | ✅ | ✅ | ✅ | Foto não mexe em valores; vendedor costuma fotografar |
| `/carros/{id}/fotos` | POST | ✅ | ✅ | ✅ | |
| `/carros/{id}/fotos/{fotoId}` | PUT | ✅ | ✅ | ✅ | |
| `/carros/{id}/foto`, `/carros/{id}/fotos/{fotoId}` | DELETE | ❌ | ✅ | ✅ | |

### Clientes e dashboard

| Rota | Método | VENDEDOR | GERENTE | ADMIN | Observação |
|---|---|:-:|:-:|:-:|---|
| `/clientes`, `/clientes/{id}` | GET | ✅ | ✅ | ✅ | |
| `/clientes` | POST | ✅ | ✅ | ✅ | Vendedor cadastra o cliente no atendimento |
| `/clientes/{id}` | PUT | ✅ | ✅ | ✅ | |
| `/clientes/{id}/ativo` | PUT, DELETE | ❌ | ✅ | ✅ | Inativar impede compras |
| `/clientes/{id}` | DELETE | ❌ | ❌ | ✅ | Exclusão física de dado pessoal |
| `/dashboard` | GET | ✅ | ✅ | ✅ | Custos ocultos para VENDEDOR (Etapa 1a) |

### Propostas

| Rota | Método | VENDEDOR | GERENTE | ADMIN | Observação |
|---|---|:-:|:-:|:-:|---|
| `/propostas`, `/propostas/{id}` | GET | ✅ | ✅ | ✅ | Ver decisão D3 (ver todas vs. só as próprias) |
| `/propostas/simulacoes` | POST | ✅ | ✅ | ✅ | Não grava nada |
| `/propostas` | POST | ✅ | ✅ | ✅ | O vendedor da proposta passa a ser o usuário do token |
| `/propostas/{id}/revisoes` | POST | própria | ✅ | ✅ | |
| `/propostas/{id}/reservar` | POST | própria | ✅ | ✅ | |
| `/propostas/{id}/aprovacoes` | POST | ❌ | ✅ | ✅ | Conflito de interesse continua no domínio |
| `/propostas/{id}/converter-venda` | POST | própria | ✅ | ✅ | Domínio já exige proposta APROVADA, revisão atual e não expirada |
| `/propostas/politica-vigente` | GET | ❌ | ✅ | ✅ | Regra de URL + regra de domínio da Etapa 2 |
| `/propostas/politicas` | POST | ❌ | ✅ | ✅ | |

### Vendas

| Rota | Método | VENDEDOR | GERENTE | ADMIN | Observação |
|---|---|:-:|:-:|:-:|---|
| `/vendas`, `/vendas/{id}` | GET | ✅ | ✅ | ✅ | `VendaModel` não expõe custo |
| `/vendas` | POST | ❌ | ✅ | ✅ | Venda direta, sem proposta (SEC-03). O VENDEDOR vende pelo `converter-venda`, que o frontend já usa |
| `/vendas/{id}` | PUT | ❌ | ✅ | ✅ | |
| `/vendas/{id}/cancelamento-auditado` | POST | ❌ | ✅ | ✅ | Responsável = usuário do token |
| `/vendas/{id}` | DELETE | ❌ | ❌ | ✅ | Obsoleto desde a Etapa 2; remoção definitiva na Fase 9 |

### Autenticação e infraestrutura

| Rota | Acesso | Observação |
|---|---|---|
| `/auth/login` | público | |
| `/auth/refresh`, `/auth/logout` | público + cookie + `X-Requested-With` | |
| `/auth/me`, `/auth/senha` | autenticado | |
| `/swagger-ui/**`, `/v3/api-docs/**` | público | Restringir por perfil de execução fica na Etapa 4 (SEC-12) |
| `/error` | público | Necessário para o encaminhamento interno de erros do Spring |
| qualquer outra rota | **negada** | `anyRequest().denyAll()` — rota nova sem regra explícita falha fechada |

### Gestão de usuários

Na Etapa 3, a API ganha `GET/POST /usuarios`, `PUT /usuarios/{id}` (nome, perfil), `PUT/DELETE /usuarios/{id}/ativo` e `POST /usuarios/{id}/senha-temporaria`, **todos só para ADMIN**. A tela de administração de usuários fica para depois (decisão D6).

---

## 6. CORS centralizado

- Remover `@CrossOrigin(origins = "*")` dos 6 controllers.
- Um `CorsConfigurationSource` com origens vindas de `app.cors.allowed-origins`. **Padrão: lista vazia** (nenhuma origem cruzada), porque o painel é servido na **mesma origem** que a API: o Nginx faz o proxy de `/api` em produção e o Vite faz o mesmo em desenvolvimento.
- Se alguém precisar de outra origem, ela é declarada explicitamente, com `Authorization`, `Content-Type` e `X-Requested-With` como cabeçalhos permitidos e `allowCredentials=true` (necessário para o cookie de refresh).

---

## 7. Swagger com Bearer

- `SwaggerConfig` declara um `SecurityScheme` HTTP `bearer` (formato JWT) e o aplica a todas as operações, exceto as de `/auth`.
- Uso: `POST /auth/login` no próprio Swagger → copiar `accessToken` → botão **Authorize**.
- Swagger continua público nesta etapa; a restrição por ambiente fica na Etapa 4.

---

## 8. Frontend (React)

### 8.1 Tela de login

```
┌───────────────────────────────────────────────┐
│                                               │
│               [logo Gomez]                    │
│        Painel do funcionário                  │
│                                               │
│   E-mail                                      │
│   ┌───────────────────────────────────────┐   │
│   │ nome@concessionariagomez.com.br       │   │
│   └───────────────────────────────────────┘   │
│   Senha                                       │
│   ┌──────────────────────────────────┬────┐   │
│   │ ••••••••••                       │ 👁 │   │
│   └──────────────────────────────────┴────┘   │
│                                               │
│   ┌───────────────────────────────────────┐   │
│   │               Entrar                  │   │
│   └───────────────────────────────────────┘   │
│   E-mail ou senha inválidos.   (quando falha) │
│                                               │
└───────────────────────────────────────────────┘
```

- Mesmo tema claro/escuro do painel. `autocomplete="username"` e `"current-password"` para o gerenciador de senhas funcionar.
- Botão com estado "Entrando…" e bloqueio de duplo envio. Sem "lembrar de mim": a sessão dura um turno (10 h).
- Ao abrir o app: tela de carregamento curta enquanto `/auth/refresh` tenta recuperar a sessão. Se funcionar, o usuário cai direto no painel.

### 8.2 Usuário logado e logout

O `<select>` "Perfil do operador comercial" do cabeçalho é **removido** e substituído por:

```
                                   ┌──────────────────────────────┐
  … cabeçalho do painel …          │ (AG) André Gomez  [GERENTE] ▾│
                                   └──────────────────────────────┘
                                     ├ Trocar senha
                                     └ Sair
```

- Iniciais, nome e um selo com o perfil.
- **Sair:** chama `/auth/logout`, limpa o token em memória, cancela requisições em andamento e volta para o login.
- **Sessão expirada** (renovação falhou): volta ao login com o aviso *"Sua sessão expirou. Entre novamente."*

### 8.3 Mudanças em `api.ts`

- Remove `setOperadorContext`, `getOperadorContext` e os cabeçalhos `X-Operador-*`.
- `request()` envia `Authorization: Bearer <token>`. Em **401**, tenta **uma** renovação (com uma única renovação em andamento compartilhada entre requisições simultâneas) e repete a requisição. Se a renovação falhar, dispara o logout.
- Um `AuthProvider` (React context) expõe `usuario`, `login()`, `logout()` e `status`. O `perfil` que hoje vem do seletor passa a vir de `usuario.perfil`. `Stock.tsx` e `Proposals.tsx` já recebem o perfil, então a troca é localizada.

### 8.4 Fotos

`<img src>` não envia o cabeçalho `Authorization`, então as fotos deixariam de carregar.

- **Recomendado:** um hook `useImagemAutenticada(url)` busca a imagem com o token, cria um `URL.createObjectURL` e o libera ao desmontar. Substitui `photoUrl` nos 5 pontos de uso (`Stock.tsx` ×2, `VehicleGallery.tsx` ×2, `VehicleImage.tsx`). Toda a API continua autenticada, sem exceção.
- **Alternativa:** liberar sem autenticação apenas os GETs de conteúdo das fotos. Menos código, porém abre uma exceção na regra "tudo autenticado" e permite enumerar veículos pelo ID.

### 8.5 Botões conforme o perfil

O backend é a fonte da verdade. O frontend apenas esconde o que daria 403, para não oferecer ações impossíveis:

| Ação na tela | Visível para |
|---|---|
| Novo veículo, Editar veículo, Ativar/Inativar veículo | GERENTE, ADMIN |
| Excluir veículo, Excluir cliente | ADMIN |
| Nova venda direta, Editar venda, Cancelar venda | GERENTE, ADMIN |
| Aprovar/Rejeitar proposta | GERENTE, ADMIN |
| Revisar, Reservar e Converter proposta | dono da proposta, GERENTE, ADMIN |

---

## 9. Testes

- **`SegurancaMatrizIT` (novo, parametrizado):** cada linha da matriz da seção 5 vira um caso `(método, rota, perfil) → status esperado`, incluindo "sem token → 401". Isso garante que o documento e o código não divirjam.
- **`AutenticacaoIT` (novo):** login certo/errado, mensagem única, bloqueio após 5 falhas, refresh com rotação, reuso de refresh revogado, logout, usuário desativado, troca de senha.
- **Unitários:** emissão e leitura do JWT, carga inicial (não sobrescreve usuário existente), regra de "proposta própria".
- **ITs atuais:**
  - MockMvc (`PoliticaComercialIT`): `.with(jwt().authorities(...))` do `spring-security-test`.
  - RestAssured (`CadastroCarroIT`, `GaleriaCarroIT`): um utilitário de teste gera o token com o `JwtEncoder` da própria aplicação. Os `.header(PERFIL, GERENTE)` atuais viram `.auth().oauth2(token(GERENTE))`.
- `application-test.properties` recebe um `JWT_SECRET` fixo, **só de teste**.
- **Frontend:** testes de `api.ts` para "401 → renova → repete", "renovação falha → logout" e "uma única renovação para requisições simultâneas".
- **Smoke test:** como toda rota passa a exigir token, o smoke test precisa de um `POST /api/auth/login` com o usuário de teste. Isso grava `ultimo_login` e uma sessão, mas **não altera dados de negócio**. Proponho abrir essa exceção à regra "só GETs" (decisão D8).

---

## 10. Operação e documentação

- `.env.example`: `JWT_SECRET`, `SEED_ADMIN_PASSWORD`, `SEED_GERENTE_PASSWORD`, `SEED_VENDEDOR_PASSWORD`, com o comando para gerar o segredo:
  `[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])`
- `docker-compose.yml`: repassa essas variáveis ao backend, com `${JWT_SECRET:?...}` para falhar se faltar.
- README e `docs/GUIA_DEVOPS_E_DOCKER.md`: primeiro acesso, troca de senha e como resetar uma senha esquecida (ADMIN pela API).
- Postman: requisição "0. Login" que salva o token numa variável da coleção; todas as outras usam `Bearer {{token}}`. Os cabeçalhos `X-Operador-*` saem.
- `CHANGELOG` e `graphify update .`, como nas outras etapas.
- **Efeito colateral bom:** o Spring Security já adiciona `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` e `Cache-Control: no-store` nas respostas da API. Isso adianta parte da Etapa 4 (SEC-10).

---

## 11. Divisão em duas entregas

A Etapa 3 é grande. Para o diff continuar revisável, proponho duas entregas. O painel funciona ao fim de cada uma.

| Entrega | Conteúdo |
|---|---|
| **3a — Núcleo** | `V011`, usuários, carga inicial, `/auth/*` (login, refresh, logout, me), JWT, matriz de permissões, `OperadorAutenticado` no lugar dos cabeçalhos, 401/403 no formato Problem, CORS global, Swagger com Bearer, bloqueio por tentativas, todos os testes. Frontend: login, usuário logado + Sair, `Authorization` + renovação automática, fotos autenticadas, remoção do seletor de perfil. |
| **3b — Complementos** | `PUT /auth/senha` + tela "Trocar senha", API de gestão de usuários (ADMIN), botões conforme o perfil (seção 8.5), Postman com login, README/guia completos. |

---

## 12. Decisões que dependem de aprovação

| # | Decisão | Recomendação | Alternativa |
|---|---|---|---|
| D1 | Biblioteca do JWT | Resource Server (Nimbus) | `jjwt` |
| D2 | Onde guardar o token | Access em memória (15 min) + refresh em cookie HttpOnly (10 h, rotativo) | Access de 8 h em `sessionStorage`, sem refresh |
| D3 | Propostas para VENDEDOR | Vê todas, altera só as próprias | Vê e altera só as próprias |
| D4 | Exclusões físicas (carro, cliente, venda legada) | Só ADMIN | GERENTE e ADMIN |
| D5 | Fotos | Hook com blob autenticado | GET de conteúdo público |
| D6 | Gestão de usuários | Só API (ADMIN) na Etapa 3; tela depois | Tela já na 3b |
| D7 | Swagger | Público até a Etapa 4 | Exigir login já na Etapa 3 |
| D8 | Smoke test | Permitir `POST /api/auth/login` | Manter só GETs (o smoke test perde as rotas autenticadas) |
| D9 | Entregas | 3a e 3b separadas | Etapa 3 inteira de uma vez |
