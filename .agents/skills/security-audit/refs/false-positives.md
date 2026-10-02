# Guia Anti-Falso-Positivo

Para garantir a credibilidade do relatório de segurança, **todo achado preliminar deve ser confrontado com as regras abaixo antes de ser incluído no relatório**. Se enquadrar nas regras de descarte, deve ser rebaixado para **INFO** ou descartado da lista de vulnerabilidades.

---

## 1. Casos Comuns no Ecossistema Spring Boot

### 1.1 Injeção de SQL Inexistente
- **Falso positivo:** Apontar SQL Injection em métodos como `findByAno(Integer ano)` ou `findByModeloContainingIgnoreCase(String modelo)`.
- **Motivo técnico:** O Spring Data JPA traduz métodos derivados diretamente para JPQL parametrizado com `PreparedStatement`. Não há risco de concatenação de string.
- **Ação:** Descartar imediatamente.

### 1.2 Swagger UI / SpringDoc em Ambiente de Desenvolvimento
- **Falso positivo:** Classificar `/swagger-ui/index.html` como falha de segurança **HIGH** em projeto de desenvolvimento local.
- **Motivo técnico:** É uma ferramenta padrão de produtividade e documentação durante o ciclo de desenvolvimento.
- **Ação:** Rebaixar para **INFO**, sugerindo apenas restringir ou desativar o endpoint no perfil de produção (`application-prod.properties` via `springdoc.swagger-ui.enabled=false`).

### 1.3 Dados Fictícios em Scripts de Teste (`afterMigrate.sql`)
- **Falso positivo:** Reportar que há registros de carros ou dados mockados no arquivo `afterMigrate.sql` como "vazamento de dados privados".
- **Motivo técnico:** São dados fictícios de seed (fixtures) para testes de integração e uso em desenvolvimento local.
- **Ação:** Descartar imediatamente.

### 1.4 Testes Automatizados (`src/test/java`)
- **Falso positivo:** Apontar senhas em texto puro ou IDs estáticos em classes como `CadastroCarroIT.java` ou testes unitários.
- **Motivo técnico:** Testes de integração utilizam dados estáticos controlados em ambiente isolado.
- **Ação:** Descartar. Não auditar classes de teste com regras de produção.

### 1.5 "Dependência Possui Versão Mais Nova"
- **Falso positivo:** Marcar dependência como vulnerável (HIGH/MEDIUM) apenas porque existe versão mais recente no Maven Central ou npm registry.
- **Motivo técnico:** Sem um CVE ou boletim de segurança (GHSA) específico que afete o método utilizado, trata-se de atualização de rotina, não de falha de segurança.
- **Ação:** Registrar no máximo como sugestão **INFO**.

---

## 2. Casos no Frontend (React)

### 2.1 `VITE_API_URL` ou URL do Backend no Código
- **Falso positivo:** Tratar a URL da API (ex: `http://localhost:8080`) como "segredo vazado".
- **Motivo técnico:** O endereço público da API é consumido pelo navegador do cliente e precisa ser público para que as requisições ocorram.
- **Ação:** Descartar.
