# 🚗 Concessionária Gomez — Plataforma Integrada

Sistema completo de gestão para concessionária de veículos, composto por uma **API RESTful de alta maturidade em Spring Boot 3 + Java 21**, banco de dados relacional **MySQL 8.0** e um **painel web SPA moderno em React + TypeScript + Tailwind CSS**, 100% orquestrado com **Docker e Docker Compose**.

---

## ⚡ Como Rodar Tudo em 1 Comando

Com o Docker Desktop instalado e aberto na sua máquina:

```powershell
docker compose up -d
```

Pronto! Os três serviços sobem em segundo plano automaticamente na ordem correta:

| Aplicação | URL de Acesso | O que é |
|---|---|---|
| 🌐 **Painel Web (Frontend)** | **[http://localhost](http://localhost)** | Interface gráfica do funcionário (React SPA) |
| 📖 **Swagger UI (Docs da API)** | **[http://localhost/swagger-ui/index.html](http://localhost/swagger-ui/index.html)** | Documentação OpenAPI 3 viva e interativa |
| ⚡ **API Spring Boot** | **[http://localhost:8080/dashboard](http://localhost:8080/dashboard)** | Endpoints RESTful diretos |
| 🗄️ **Banco MySQL** | `localhost:3307` | Conexão para DBeaver / DataGrip (`root` / `catita01`) |

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
- 🚀 **[Roadmap de Evolução](ROADMAP.md):** Histórico de todas as 6 fases concluídas no projeto.
- 🎨 **[Documentação do Frontend](frontend/README.md):** Detalhes da arquitetura do painel em React.
