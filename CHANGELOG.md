# Changelog

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

---

## [Não lançado]

### Segurança
- **Etapa 1a da correção da auditoria de 25/11/2026 (contenção imediata):**
  - **Perfil padrão fail-closed (SEC-01):** `PerfilOperadorComercial.fromHeader` passa a retornar `VENDEDOR` quando o cabeçalho `X-Operador-Perfil` está ausente ou vazio (antes retornava `GERENTE`). O frontend não é afetado, pois sempre envia o cabeçalho. O teste de integração do resumo (`GaleriaCarroIT`) passou a declarar o perfil `GERENTE` e ganhou um cenário que confirma a ocultação de custos sem o cabeçalho.
  - **Receita adicional declarada pelo vendedor (SEC-14):** itens com base `RECEITA_ADICIONAL_ELEGIVEL` enviados por quem não tem alçada gerencial entram pendentes de confirmação no `PropostaComercialAssembler`. Isso impede que o vendedor zere o piso de margem para obter aprovação automática. A confirmação ocorre por uma nova revisão enviada por `GERENTE`/`ADMIN`.
  - **Venda dupla (SEC-05):** `DELETE /vendas/{id}` passa a recusar a exclusão de venda já cancelada com auditoria, que antes reativava um veículo possivelmente já revendido.
  - **Autoaprovação na tela de propostas:** removida a troca automática do decisor por um identificador fictício de "diretoria" em `Proposals.tsx` e o parâmetro `operadorOverrideId` em `api.ts`. Quando o decisor é o próprio vendedor da proposta, o backend recusa a decisão por conflito de interesse.
  - **Docker Compose:** portas publicadas apenas em `127.0.0.1`; credenciais lidas de um arquivo `.env` não versionado (modelo em `.env.example`); a API passa a conectar com o usuário dedicado `concessionaria_app`, restrito ao schema `concessionaria`, em vez de `root` (SEC-07 e SEC-11). README e `docs/GUIA_DEVOPS_E_DOCKER.md` atualizados, incluindo como criar o usuário em um volume de banco já existente.
  - Novos testes unitários: `PerfilOperadorComercialTest`, `PropostaComercialAssemblerTest` e o cenário de venda cancelada em `EmissaoVendaServiceTest`.
  - *Motivação:* Fechar os pontos de abuso que não dependem da implantação do Spring Security (Fase 8) e reduzir a exposição do ambiente local.
- **Etapa 1b da correção da auditoria de 25/11/2026 (dependências — SEC-08):**
  - Spring Boot `3.3.4` → `3.5.16`, a última versão da linha 3.x. Com isso, o Tomcat embarcado sobe de `10.1.30` para `10.1.55`, corrigindo a **CVE-2025-48988** (DoS em upload `multipart/form-data`, corrigida a partir da `10.1.42`).
  - Também atualizados: Spring Framework `6.1.13` → `6.2.19`, Hibernate `6.5.3` → `6.6.53`, Flyway → `11.7.2` e SpringDoc `2.6.0` → `2.8.17`, a linha compatível com o Boot 3.5. O Lombok passa a seguir a versão gerenciada pelo Boot (`1.18.46`).
  - Removida a propriedade `spring.jpa.properties.hibernate.dialect`, depreciada (`HHH90000025`). O Hibernate detecta o dialeto MySQL automaticamente.
  - Sem alteração de código de produção nem de comportamento da API.
  - *Atenção:* o suporte gratuito (OSS) da linha 3.5 terminou em 30/06/2026. A migração para Spring Boot 4.x deve ser planejada como etapa própria.
- **Etapa 2 da correção da auditoria de 25/11/2026 (integridade de estado e confidencialidade):**
  - **Ciclo de vida das vendas (SEC-03, parte de domínio):** a venda direta (`POST /vendas` sem proposta) e a troca de veículo em `PUT /vendas/{id}` recusam veículo com reserva vigente de proposta (HTTP 409). `PUT /vendas/{id}` recusa vendas canceladas e vendas originadas de proposta aprovada, cujos valores são congelados. `DELETE /vendas/{id}` fica **obsoleto** (marcado como `deprecated` no OpenAPI) e recusa vendas originadas de proposta; o caminho suportado é `POST /vendas/{id}/cancelamento-auditado`. A restrição por papel (quem pode emitir, alterar ou cancelar) fica para a Etapa 3, com o Spring Security.
  - **Venda dupla (SEC-05, conclusão):** o veículo só volta ao estoque, seja pela exclusão legada, pela troca de veículo ou pelo cancelamento auditado, se não houver **outra** venda vigente para ele. O atalho `PUT /carros/{id}/ativo` passou a recusar veículos com venda vigente; para devolvê-los ao estoque é preciso cancelar a venda.
  - **Custo de aquisição (SEC-04):** `compra` é devolvido como `null` em `/carros` (listagem, consulta, cadastro e alteração) para quem não é `GERENTE`/`ADMIN`, incluindo chamadas sem o cabeçalho de perfil. Os filtros `minValor`/`maxValor` e a ordenação `sort=compra` respondem **403** para esses perfis, pois permitiriam deduzir o custo por busca binária. Removida a consulta não utilizada `CarroRepository.findComFiltros`, que filtrava pelo custo.
  - **Política comercial (SEC-04 e SEC-15):** `GET /propostas/politica-vigente` passou a ser restrito a `GERENTE`/`ADMIN` (403 para os demais), e ele e `POST /propostas/politicas` devolvem o novo DTO `PoliticaComercialModel`, que referencia o veículo apenas por `carroId`. Antes devolviam a entidade JPA, que serializava o `Carro` com o custo.
  - **Cancelamento auditado:** o motivo passa a ter no máximo 255 caracteres (tamanho da coluna). Antes, um texto maior gerava erro 500.
  - **Frontend:** o cancelamento na tela de Vendas usa o endpoint auditado e exige o motivo. Vendas canceladas continuam listadas, com o selo "Cancelada" e o motivo, e sem ações. Vendas originadas de proposta não exibem "Editar". O histórico do veículo destaca vendas canceladas. Para `VENDEDOR`, a tela de Estoque oculta os filtros de compra mínima/máxima e a ordenação por valor de compra.
  - **Postman:** as requisições que filtram ou ordenam por custo enviam `X-Operador-Perfil: GERENTE`, e "Cancelar Venda" passou a usar o cancelamento auditado.
  - Novos testes: `CadastroCarroServiceTest` e `PoliticaComercialIT`; novos cenários em `EmissaoVendaServiceTest`, `CarroModelAssemblerTest`, `CarroControllerTest`, `CadastroCarroIT` e `api.test.ts`.
  - *Risco residual conhecido:* o preço sugerido (`venda`) é calculado como `compra × 1,10` em código público, de modo que o custo ainda pode ser deduzido a partir dele. A decisão sobre esse ponto está pendente.
- **Etapa 3a da correção da auditoria de 25/11/2026 (autenticação e autorização — núcleo; SEC-01, SEC-02, SEC-03 e SEC-06):**
  - **Spring Security com JWT (Resource Server):** todas as rotas exigem `Authorization: Bearer <token>`, exceto `POST /auth/login`, `/auth/refresh`, `/auth/logout` e a documentação Swagger, que continua pública até a Etapa 4. O token é HS256, assinado com `JWT_SECRET` do `.env` (sem valor padrão: sem a chave, ou com chave menor que 256 bits, a aplicação não sobe), e vale 15 minutos. Sem token ou com token inválido, a resposta é **401**; com perfil insuficiente, **403**. Ambas usam o formato Problem Details da API.
  - **Os cabeçalhos `X-Operador-Perfil` e `X-Operador-Id` deixaram de existir.** Perfil, e-mail e nome do operador vêm do token, injetados nos controllers como `OperadorAutenticado`. Os registros de auditoria (responsável pela proposta, decisor da aprovação, responsável pelo cancelamento) passam a gravar o e-mail do usuário autenticado. Foram removidos os e-mails fixos usados como valor padrão quando o operador não era informado.
  - **Matriz de permissões por rota** em `SecurityConfig`, seguindo a seção 5 de `docs/security/design/2026-11-25-etapa3-autenticacao-rbac.md`. Toda rota sem regra explícita é negada (`denyAll`). Cadastro e edição de veículos, aprovação de propostas, política comercial, emissão, alteração e cancelamento de vendas, ativação/inativação e exclusão de fotos exigem `GERENTE`/`ADMIN`. Exclusões definitivas de veículos, clientes e vendas exigem `ADMIN`.
  - **Vendedor só conduz as próprias propostas:** `VENDEDOR` consulta todas, mas criar revisão, reservar o veículo e converter em venda exigem que a proposta seja dele (403 caso contrário). `GERENTE`/`ADMIN` operam qualquer proposta.
  - **Login com bloqueio:** `POST /auth/login` (e-mail e senha) devolve o access token no corpo. Toda falha, seja e-mail inexistente, senha errada, conta bloqueada ou inativa, recebe a mesma mensagem genérica, e o tempo de resposta não revela se o e-mail existe. Após 5 senhas erradas seguidas, a conta fica bloqueada por 15 minutos. Senhas são guardadas com BCrypt (`{bcrypt}`); uma senha acima de 72 bytes é tratada como senha errada, não como erro interno.
  - **Sessão renovável e revogável:** o refresh token é um valor aleatório de 256 bits entregue só num cookie `HttpOnly`, `SameSite=Strict`, `Secure` (configurável por `AUTH_COOKIE_SECURE`) e restrito a `/api/auth`. O banco guarda apenas o seu hash SHA-256. A sessão dura no máximo 10 horas e é rotacionada a cada renovação. Reapresentar um token já rotacionado dentro de 10 segundos (corrida legítima entre abas) devolve só um novo access token. Fora dessa janela, o reuso revoga todas as sessões do usuário (indício de roubo). `/auth/refresh` e `/auth/logout` exigem o cabeçalho `X-Requested-With: gomez-spa`, que um formulário de outro site não consegue enviar. `GET /auth/me` devolve o usuário autenticado.
  - **Migration `V011`:** cria as tabelas `usuario` e `sessao_usuario`. Não altera dados existentes; o volume `mysql_data` é preservado.
  - **Usuários iniciais:** na subida, são criados `admin@`, `gerente@` e `vendedor@concessionariagomez.com.br`, os mesmos e-mails já gravados no histórico, com as senhas de `SEED_*_PASSWORD` do `.env` (10 a 72 caracteres). Um usuário que já existe nunca é alterado. Nenhuma senha fica no repositório.
  - **CORS global:** removidos os `@CrossOrigin` dos controllers. Por padrão, nenhuma origem cruzada é aceita, porque o painel usa a mesma origem via proxy. Origens podem ser liberadas por `CORS_ALLOWED_ORIGINS`.
  - **Frontend:**
    - Tela de login (e-mail, senha com mostrar/ocultar, "Entrando…", erro genérico, `autocomplete` para gerenciadores de senha).
    - O seletor de perfil deu lugar a um chip com as iniciais, o nome e o perfil do usuário, e o botão "Sair".
    - O access token fica só em memória. Ao recarregar a página, a sessão é recuperada pelo cookie numa verificação única, sem duplicar a renovação no StrictMode.
    - Em 401, as requisições simultâneas compartilham uma única renovação e são repetidas. Se a renovação falhar, a tela de login informa que a sessão expirou.
    - As fotos protegidas são baixadas com o token (`useImagemAutenticada`, via blob URL). Sem foto (404), continua aparecendo a imagem de referência do modelo.
  - **Swagger:** esquema `bearerAuth` com o botão **Authorize**; o operador autenticado não aparece mais como parâmetro.
  - **Configuração e documentação:**
    - `docker-compose.yml` e `.env.example` ganharam `JWT_SECRET`, `SEED_*_PASSWORD`, `AUTH_COOKIE_PATH` e `AUTH_COOKIE_SECURE`.
    - README e `docs/GUIA_DEVOPS_E_DOCKER.md` explicam o primeiro acesso, os perfis e os problemas comuns de login.
    - A coleção do Postman ganhou a pasta "0. Autenticação", que salva o `accessToken` usado pela coleção como Bearer, e perdeu os cabeçalhos `X-Operador-*`. A atualização completa da coleção fica para a Etapa 3b.
  - **Testes:**
    - Novos: `AutenticacaoIT` (login, bloqueio, rotação, reuso dentro e fora da janela, expiração, usuário desativado, logout, anti-CSRF e token adulterado), `SegurancaMatrizIT` (cada rota × cada perfil, mais o caso sem token) e `CargaInicialUsuariosTest`.
    - Novos cenários de propriedade da proposta em `GestaoPropostaComercialServiceTest`, e testes de sessão em `api.test.ts`.
    - `CadastroCarroIT`, `GaleriaCarroIT`, `PoliticaComercialIT` e `CarroControllerTest` passaram a autenticar com tokens reais emitidos pelo utilitário `TokensDeTeste`.
  - *Fica para a Etapa 3b:* gestão de usuários pela API (cadastro, desativação e redefinição de senha), ocultar no frontend os botões que o perfil não pode usar (hoje a API responde 403) e a atualização completa da coleção do Postman.
- **Etapa 3b da correção da auditoria de 25/11/2026 (autenticação e autorização — complementos):**
  - **Trocar a própria senha (`PUT /auth/senha`):** qualquer usuário autenticado troca a senha informando a atual e a nova (10 a 72 caracteres, diferente da atual).
    - Senha atual errada responde **400** (não 401, para não derrubar a sessão do painel) e conta para o bloqueio de 5 tentativas.
    - A troca encerra as outras sessões do usuário e mantém a atual, identificada pelo cookie. Sem o cookie, encerra todas.
  - **Gestão de usuários (`/usuarios`, somente `ADMIN`):**
    - Listar (paginado, por nome), consultar, cadastrar, alterar nome e perfil, inativar (`DELETE /usuarios/{id}/ativo`) e reativar (`PUT /usuarios/{id}/ativo`).
    - `POST /usuarios/{id}/senha-temporaria` atende quem esqueceu a senha.
    - O administrador nunca escolhe a senha de outra pessoa. O cadastro e a redefinição geram no servidor uma senha temporária aleatória de 16 caracteres, devolvida uma única vez com `Cache-Control: no-store`.
    - O e-mail é normalizado (sem espaços, minúsculo) e único (**409** se repetido). Não pode ser alterado depois, porque identifica o autor nos registros de auditoria.
    - Reativar e gerar senha temporária zeram `tentativas_falhas` e `bloqueado_ate`. Inativar e gerar senha temporária encerram as sessões do usuário.
    - O `ADMIN` não pode inativar a si mesmo, alterar o próprio perfil nem gerar senha temporária para si (deve usar "Trocar senha"). Assim o sistema nunca perde o último administrador por engano.
    - A mudança de perfil vale na próxima renovação do token (até 15 minutos).
    - A resposta nunca inclui o hash da senha.
  - **`PoliticaSenha`:** as regras de tamanho (10 a 72 caracteres, limite de 72 bytes do BCrypt) e a geração da senha temporária ficam num só lugar, usado pelo login, pela troca de senha, pela gestão de usuários e pela carga inicial.
  - **Frontend:**
    - O chip do usuário virou um menu com o e-mail, "Trocar senha" e "Sair".
    - O diálogo de troca de senha valida tamanho, confirmação e senha igual à atual antes de enviar, e permite mostrar ou ocultar as senhas.
    - Os botões que o perfil não pode usar deixaram de aparecer (a API continua respondendo 403):
      - `VENDEDOR` não vê "Novo veículo", "Nova venda", editar ou ativar/inativar veículo, ativar/inativar cliente, editar ou cancelar venda, nem remover foto.
      - Só `ADMIN` vê a exclusão definitiva de veículos e clientes.
      - Nas propostas, "Nova revisão" e "Fechar venda" aparecem para o vendedor dono da proposta e para `GERENTE`/`ADMIN`.
  - **Swagger:** o requisito `bearerAuth` passa a ser dispensado só em `/auth/login`, `/auth/refresh` e `/auth/logout`, e não mais em todo o prefixo `/auth`. Com isso, `/auth/me` e `/auth/senha` mostram o cadeado.
  - **Postman:** "0.5 Trocar a própria senha" na pasta de autenticação e a nova pasta "Usuários (ADMIN)" com as 7 rotas. O cadastro guarda o `usuarioId` e confere o `Cache-Control: no-store`.
  - **Documentação:** README e `docs/GUIA_DEVOPS_E_DOCKER.md` explicam como trocar a senha e como o administrador desbloqueia ou redefine a senha de um usuário.
  - **Testes:**
    - Novos: `UsuarioIT` (cadastro, e-mail duplicado ou inválido, listagem, mudança de perfil efetiva na renovação, autoproteções, inativação e reativação com desbloqueio, senha temporária) e `PoliticaSenhaTest`.
    - Novos cenários de troca de senha em `AutenticacaoIT`, e testes de permissões e validação de senha em `permissoes.test.ts`.
    - `SegurancaMatrizIT` cobre as novas rotas. Nos casos permitidos, passa a exigir uma resposta diferente de 401/403 **e menor que 500**. A preparação roda uma vez por classe (`@BeforeAll`), e o tempo caiu de cerca de 2 minutos para poucos segundos.
- **Etapa 4 da correção da auditoria de 25/11/2026 (hardening — SEC-07, SEC-09, SEC-10, SEC-12, SEC-13 e SEC-16):**
  - **Erros sem detalhes internos (SEC-09):**
    - Falhas de disco nas fotos (`DiscoLocalFotoStorageService` e upload em `CarroFotoController`) vão para o log com a causa completa. O cliente recebe uma mensagem genérica; antes, a mensagem da `IOException` expunha o caminho no servidor (`/app/catalogo-fotos/...`).
    - O `ApiExceptionHandler` registra erros inesperados com SLF4J em vez de `printStackTrace()`.
    - `?sort=` com propriedade inexistente (`sort=foo`, `sort=COMPRA`) responde **400** (`PARAMETRO_INVALIDO`) em vez de 500.
  - **Cabeçalhos de segurança (SEC-10):**
    - `GET /carros/{id}/foto` envia `X-Content-Type-Options: nosniff` explicitamente, como a galeria, e monta o `Content-Disposition` com `ContentDisposition`.
    - O Nginx deixa de mostrar a versão (`server_tokens off`).
    - Todas as respostas levam `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` e `Content-Security-Policy`.
    - A CSP do painel aceita scripts só da própria origem (`script-src 'self'`), imagens `blob:`/`data:` (fotos baixadas com token) e conexões com `https://viacep.com.br` (CEP).
    - O script inline que aplica o tema saiu do `index.html` para `public/tema.js`. O Swagger tem uma CSP própria.
    - Os cabeçalhos ficam em `frontend/nginx/*.conf` e são incluídos em todo `location` que tem `add_header` próprio.
  - **Nginx sem root (SEC-16):** a imagem passa a ser `nginxinc/nginx-unprivileged:alpine`, que roda com o UID 101 e escuta na porta 8080. O compose publica `127.0.0.1:80:8080`, e os arquivos do site ficam somente leitura para o processo.
  - **Links HATEOAS atrás do `/api`:**
    - `server.forward-headers-strategy=framework`: os links usam `X-Forwarded-Proto/Host/Prefix` e saem como `http://localhost/api/carros/1` (antes faltava o `/api`).
    - O Nginx sobrescreve esses cabeçalhos e apaga `Forwarded`, `X-Forwarded-Port` e `X-Forwarded-Ssl`, que o cliente poderia forjar.
    - O proxy do Vite envia os mesmos cabeçalhos (`xfwd` + `X-Forwarded-Prefix`).
    - O Swagger (perfil `dev`) passa a anunciar o servidor `http://localhost/api`. Antes anunciava `http://localhost`, e o "Try it out" chamava `/carros`, que devolvia a página da SPA.
  - **Perfis `dev` e `prod` (SEC-12 e SEC-13):**
    - A configuração base passa a ter padrões seguros: `show-sql=false` e `server.error.include-message=never`.
    - O perfil `dev` liga o SQL no log e as mensagens detalhadas.
    - O perfil `prod` desliga o Swagger e o `/v3/api-docs`, e é o padrão do `docker-compose.yml` (`SPRING_PROFILES_ACTIVE`).
  - **Nenhuma senha versionada (SEC-07, complemento):**
    - Removido o fallback de senha do banco no `application.properties`: sem credenciais, a API não sobe. Fora do Docker, a API lê o `.env` da raiz (`spring.config.import=optional:file:.env[.properties]`).
    - `application-test.properties` usa `TEST_DB_USERNAME`/`TEST_DB_PASSWORD`, com o `MYSQL_ROOT_PASSWORD` do `.env` como alternativa.
    - O CI usa uma senha neutra, exclusiva do banco descartável.
    - Removido o `flyway.properties`, que ninguém lia e era empacotado no jar com a senha antiga.
  - **Frontend:** `trocarSenha` envia `credentials: 'include'` explicitamente, por simetria com `session.ts`.
  - **Documentação:**
    - README: seção de perfis.
    - `docs/GUIA_DEVOPS_E_DOCKER.md`: porta interna 8080, Problema 8 (Swagger 404 no `prod`) e Problema 9 (senha do MySQL nos testes).
    - `.env.example`: `SPRING_PROFILES_ACTIVE` e `TEST_DB_PASSWORD`.
    - `ROADMAP.md`: Fase 8 concluída e próximos passos.
  - **Testes:**
    - Novo `DiscoLocalFotoStorageServiceTest`.
    - Em `CadastroCarroIT`: ordenação inválida responde 400, e os links usam o endereço do proxy.
    - Em `GaleriaCarroIT`: verificação do `nosniff` e do `Content-Disposition` da foto principal.
  - *Atenção:* a senha antiga continua no histórico do Git, que é público. Se ela ainda for usada em algum lugar (por exemplo, como senha do root de um volume MySQL criado antes do `.env`), troque-a.

### Modificado
- **Injeção de Dependências por Construtor (Lombok `@RequiredArgsConstructor`):**
  - Substituída a injeção por campo (`@Autowired`) por campos `private final` com `@RequiredArgsConstructor` em 16 classes de produção: controllers (`CarroController`, `CarroFotoController`, `ClienteController`, `VendaController`), services (`CadastroCarroService`, `CadastroClienteService`, `CatalogoFotoCarroService`, `EmissaoVendaService`), assemblers (`CarroModelAssembler`, `CarroInputDisassembler`, `ClienteModelAssembler`, `ClienteInputDisassembler`, `FotoCarroModelAssembler`, `VendaModelAssembler`), `ApiExceptionHandler` e `DiscoLocalFotoStorageService`.
  - Testes unitários dos quatro assemblers com testes próprios passaram a receber o `ModelMapper` pelo construtor, eliminando o uso de `ReflectionTestUtils`.
  - Sem alteração de comportamento da API.
  - *Motivação:* Dependências obrigatórias explícitas e imutáveis, falha imediata na inicialização caso algo falte, instanciação simples em testes unitários e padronização com as classes das Fases 6 e 7, que já usavam construtor.

---

## [0.1.1-BETA] - 2026-10-02

### Adicionado
- **Filtros por Faixa de Valores na Consulta de Veículos (`minValor`, `maxValor` e BETWEEN):**
  - Implementado suporte aos parâmetros de consulta `minValor` e `maxValor` no endpoint `GET /carros`.
  - Criada consulta JPQL flexível `findComFiltros` no `CarroRepository` com parametrização segura (`:minValor`, `:maxValor`, `:ano`), permitindo buscas por valor mínimo (`compra >= minValor`), valor máximo (`compra <= maxValor`), intervalo fechado (`BETWEEN`) e combinação opcional com o ano de fabricação.
  - Cobertura de testes de integração com REST-Assured em `CadastroCarroIT` expandida com 3 novos cenários validando valor mínimo, máximo e faixa entre valores.
  - Atualizada a coleção do Postman (`concessionaria-postman-collection.json`) com as requisições `1.2`, `1.3` e `1.4`.
  - *Motivação:* Permitir refinamento e busca por faixa de orçamento de veículos diretamente na rota de listagem REST, atendendo aos requisitos da Fase 3 do Roadmap.
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
