# Detecção de Segredos e Credenciais

Este documento estabelece as regras para identificar e classificar segredos expostos no repositório.

---

## 1. Arquivos Sensíveis que NÃO Devem Estar no Git

Execute verificação com `git ls-files` ou inspeção no workspace:

- `.env`, `.env.local`, `.env.prod`, `.env.production`
- Chaves privadas: `*.pem`, `*.key`, `id_rsa`, `*.pkcs8`
- Keystores Java: `*.jks`, `*.keystore`, `*.p12`
- Arquivos de credenciais de nuvem: `service-account*.json`, `credentials.json`
- Propriedades com perfis de produção: `application-prod.properties`, `application-prod.yml`

> Se qualquer arquivo acima estiver versionado no Git com credenciais ativas: **CRITICAL**.

---

## 2. Padrões de Hardcoded Secrets em Código ou Configuração

| Padrão | Descrição | Severidade |
| :--- | :--- | :--- |
| `sk_live_[0-9a-zA-Z]{24,}` | Chave de produção Stripe | **CRITICAL** |
| `AKIA[0-9A-Z]{16}` | AWS Access Key ID | **CRITICAL** |
| `-----BEGIN PRIVATE KEY-----` | Chave privada criptográfica embutida | **CRITICAL** |
| `jwt.secret` com string trivial ou hardcoded | Segredo de assinatura de tokens JWT em código | **HIGH** |
| `spring.datasource.password` em produção | Senha real de banco em arquivo de propriedades | **HIGH** |

---

## 3. Diferenciação Crucial: Ambiente Local vs Produção

Para evitar ruído e falsos positivos:

- **Credenciais locais em `docker-compose.yml` de desenvolvimento:**
  - Exemplo: `MYSQL_ROOT_PASSWORD: root` ou `MYSQL_PASSWORD: concessionaria_pass` em container mapeado apenas para porta local de teste.
  - **Classificação:** **LOW** ou **INFO** (recomendar uso de `.env.example` e injeção por variáveis de ambiente para deploy, sem alarmismo de incidente imediato).
- **Scripts de migração de teste (`afterMigrate.sql`):**
  - Dados fictícios inseridos para ambiente local de desenvolvimento (ex: carros de teste, nomes simulados).
  - **Classificação:** Não reportar como vazamento de dados, pois são fixtures de teste locais.
