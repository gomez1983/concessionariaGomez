# 🚗 Concessionária Gomez — Plataforma Integrada

Sistema completo de gestão para concessionária de veículos, composto por uma **API RESTful de alta maturidade em Spring Boot 3 + Java 21**, banco de dados relacional **MySQL 8.0** e um **painel web SPA moderno em React + TypeScript + Tailwind CSS**, 100% orquestrado com **Docker e Docker Compose**.

---

## ⚡ Como Rodar Tudo em 1 Comando

Com o Docker Desktop instalado e aberto na sua máquina:

```powershell
# Apenas na primeira vez: crie suas credenciais locais e troque as senhas do arquivo
Copy-Item .env.example .env

docker compose up -d
```

Pronto! Os três serviços sobem em segundo plano automaticamente na ordem correta:

| Aplicação | URL de Acesso | O que é |
|---|---|---|
| 🌐 **Painel Web (Frontend)** | **[http://localhost](http://localhost)** | Interface gráfica do funcionário (React SPA) |
| 📖 **Swagger UI (Docs da API)** | **[http://localhost/swagger-ui/index.html](http://localhost/swagger-ui/index.html)** | Documentação OpenAPI 3 viva e interativa (somente no perfil `dev`; veja abaixo) |
| ⚡ **API Spring Boot** | **[http://localhost:8080](http://localhost:8080)** | Endpoints RESTful diretos (exigem token; veja abaixo) |
| 🗄️ **Banco MySQL** | `localhost:3307` | Conexão para DBeaver / DataGrip (usuário e senha do seu `.env`) |

> As portas ficam acessíveis apenas nesta máquina (`127.0.0.1`).

### ⚙️ Perfis `prod` e `dev`

O `docker compose` sobe a API no perfil **`prod`**, que não expõe o Swagger/OpenAPI, não registra SQL no log e não devolve mensagens internas nos erros. Para desenvolver com o Swagger, acrescente ao `.env`:

```dotenv
SPRING_PROFILES_ACTIVE=dev
```

Depois, rode `docker compose up -d`. Fora do Docker (IntelliJ ou `mvnw spring-boot:run`), a API lê as credenciais do `.env` da raiz do projeto; ative o perfil `dev` na configuração de execução.

### 🔐 Primeiro acesso

O painel e a API exigem login. Antes da primeira subida, preencha no `.env`:

- `JWT_SECRET`: chave aleatória em Base64 (o `.env.example` traz o comando que a gera). Sem ela, o backend não sobe.
- `SEED_ADMIN_PASSWORD`, `SEED_GERENTE_PASSWORD`, `SEED_VENDEDOR_PASSWORD`: senhas de 10 a 72 caracteres.

Na subida, a API cria os usuários que ainda não existirem: `admin@`, `gerente@` e `vendedor@concessionariagomez.com.br`, com as senhas do `.env`. Depois de criados, mudar o `.env` **não** altera a senha deles.

| Perfil | Pode |
|---|---|
| **Vendedor** | Consultar estoque, clientes, propostas e vendas; cadastrar clientes; criar e conduzir **as próprias** propostas; enviar fotos. Não vê custos nem margens. |
| **Gerente** | Tudo do vendedor + cadastrar e editar veículos, aprovar propostas, configurar a política comercial, registrar e cancelar vendas. |
| **Administrador** | Tudo do gerente + exclusões definitivas e gestão de usuários. |

O painel mostra apenas as ações que o seu perfil pode executar.

Para usar a API fora do painel (Swagger ou Postman): `POST /auth/login` com `{"email": "...", "senha": "..."}`, copie o `accessToken` (vale 15 minutos) e envie `Authorization: Bearer <token>`. No Swagger, use o botão **Authorize**. Após 5 senhas erradas seguidas, a conta fica bloqueada por 15 minutos.

**Trocar a senha:** no painel, clique no seu nome (canto superior direito) → **Trocar senha**. Pela API, use `PUT /auth/senha` com `{"senhaAtual": "...", "novaSenha": "..."}`. A troca encerra as suas sessões em outros dispositivos.

**Usuários e senhas esquecidas (somente administrador):** a API `/usuarios` cadastra, altera o perfil, inativa (`DELETE /usuarios/{id}/ativo`) e reativa (`PUT /usuarios/{id}/ativo`) usuários. Ninguém escolhe a senha de outra pessoa:

- o cadastro devolve uma senha temporária uma única vez;
- para quem esqueceu a senha ou foi bloqueado, `POST /usuarios/{id}/senha-temporaria` gera uma nova senha temporária, desbloqueia a conta e encerra as sessões abertas.

Entregue a senha temporária por um canal seguro e peça à pessoa que a troque no primeiro acesso. A coleção do Postman traz essas rotas na pasta "Usuários (ADMIN)".

---

## 📋 Comandos Rápidos do Dia a Dia

```powershell
# Subir todo o sistema em segundo plano
docker compose up -d

# Recompilar tudo após alterar código Java ou React
docker compose up -d --build

# Ver logs de todos os serviços em tempo real (Ctrl + C para sair)
docker compose logs -f

# Ver logs específicos da API Java
docker compose logs -f backend

# Parar tudo com segurança (seus dados e fotos NÃO são apagados)
docker compose down

# Ver status e saúde dos containers
docker compose ps
```

> [!CAUTION]
> **Nunca** use `docker compose down -v` a menos que queira intencionalmente deletar o banco de dados e as fotos. O comando `docker compose down` (sem `-v`) preserva 100% dos seus dados.

---

## 📚 Guias Detalhados e Documentação

- 🐳 **[Guia Completo de DevOps e Comandos Docker](docs/GUIA_DEVOPS_E_DOCKER.md):** Manual completo formatado para Obsidian com explicação detalhada de cada comando, portas e troubleshooting.
- 🏛️ **[Guia de Arquitetura e Decisões de Projeto](docs/GUIA_CONCEITOS_E_ARQUITETURA.md):** Conceitos aprofundados sobre Rich Domain, HATEOAS, DTOs e testes.
- 🧪 **[Entendendo o REST-Assured](docs/ENTENDENDO_O_REST_ASSURED.md):** Guia prático sobre testes de integração automatizados.
- 🚀 **[Roadmap de Evolução](ROADMAP.md):** Histórico de todas as 8 fases concluídas no projeto e próximos passos.
- 🎨 **[Documentação do Frontend](frontend/README.md):** Detalhes da arquitetura do painel em React.
