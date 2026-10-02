---
name: security-audit
description: 'Auditoria de segurança para projetos Spring Boot (Java) e Frontend (React/Next.js/TypeScript). Rastreia vulnerabilidades com base nas versões reais instaladas (pom.xml e/ou package.json), IDOR/BOLA, SQL Injection/JPQL, Mass Assignment, exposição de segredos e vazamento de dados. Use em "auditar segurança", "security audit", "revisão de vulnerabilidades", "/security-audit [path]".'
---

Scanner orientado a **pesquisador de segurança**: analisa o contexto real da aplicação, o fluxo de dados ponta a ponta e as defesas nativas do **framework na versão exata que o projeto utiliza** — sem suposições genéricas.

**Idioma do relatório:** Português (termos técnicos OWASP, CVE e CWE podem ser mantidos em inglês).

**Escopo híbrido (Backend Spring Boot & Frontend React):**
- **Backend:** Spring Boot 3.x, Java 21+, Spring Data JPA, Hibernate, Bean Validation, Spring Security, Flyway, MySQL.
- **Frontend (se presente):** React, Vite, Next.js, TypeScript, formulários e integração com APIs.

---

## 🛡️ Princípios Fundamentais

1. **Versão Instalada > Última Lançada:** Auditar vulnerabilidades e práticas de acordo com as versões presentes no `pom.xml` / lockfile. "Existe versão mais recente" é considerado apenas **INFO**, a menos que haja um advisory ou CVE documentado.
2. **Evidência Real > Detecção de Padrão Ingênua:** Não apontar vulnerabilidade por mera presença de uma anotação ou método. Avaliar se há sanitização, validação de bean (`@Valid`) ou parametrização no caminho da requisição.
3. **Rastreamento de Fluxo (Taint Analysis):**
   `Entrada HTTP (Controller) ➔ DTO/Validação ➔ Service (Regra) ➔ Repository/DB ➔ Resposta HTTP`
4. **Filtro Rigoroso Anti-Falso-Positivo:** Todo achado deve ser checado contra `refs/false-positives.md` antes de constar no relatório final.
5. **Patches Apenas como Proposta:** Nenhuma correção é aplicada diretamente no código de forma automática. Todas são apresentadas como sugestões no formato diff `before / after`.
6. **Persistência Obrigatória de Relatório:** Todo scan deve salvar uma cópia do relatório em arquivo Markdown (`.md`) no projeto para histórico e governança.

---

## 📋 Fluxo de Execução em 8 Passos

### Step 1 — Detecção de Escopo e Stack
1. Se um caminho específico for informado (`/security-audit src/main/java`), focar apenas nele. Caso contrário, auditar a raiz do projeto.
2. Ler `refs/project-stack.md` para mapear as tecnologias ativas:
   - Checar `pom.xml` (Spring Boot, Java version, plugins, drivers).
   - Checar `package.json` / lockfile se houver módulo frontend.
3. Identificar frameworks, módulos de segurança e integrações ativas.

### Step 2 — Auditoria de Dependências
- Backend (Maven):
  - Inspecionar dependências no `pom.xml`.
  - Checar versões defasadas com CVEs conhecidas ou vulnerabilidades em bibliotecas de parsing/mapeamento (ex: Jackson, ModelMapper, etc.).
- Frontend (Node.js, se presente):
  ```bash
  npm audit --omit=dev
  ```
- Alertas de bibliotecas em `test` ou `provided` não devem ser promovidos a CRITICAL a menos que afetem a compilação/runtime de produção.

### Step 3 — Varredura de Segredos e Configurações
- Consultar `refs/secrets.md`.
- Varrer `src/main/resources/application*.properties`, `application*.yml`, `.env`, `docker-compose.yml`.
- Executar `git ls-files` para garantir que arquivos com credenciais ou chaves privadas não estejam versionados no repositório Git.

### Step 4 — Scan Profundo de Código
Carregar e aplicar os checklists pertinentes:
- **Backend:** `refs/spring-boot-checklist.md`
- **Frontend (se houver):** `refs/react-checklist.md`

| Categoria | Foco Principal |
| :--- | :--- |
| **Injeção** | SQL/JPQL raw concatenado, SpEL Injection, Command Injection |
| **AuthN / AuthZ** | BOLA / IDOR em endpoints REST (`@PathVariable id`), endpoints administrativos desprotegidos |
| **Mass Assignment** | DTOs de entrada (`@RequestBody`) aceitando IDs, datas do sistema ou campos sensíveis sem sanitização |
| **Vazamento de Dados** | Stacktraces completas em `ApiExceptionHandler`, logs expondo senhas/tokens, entidades JPA expostas diretamente sem DTO |
| **Configuração & Headers**| CORS (`@CrossOrigin` permissivo demais), Actuator endpoints expostos, CSRF em APIs sem token |
| **Integridade de Negócio**| Falta de `@Transactional` em mutações críticas, manipulação de valores monetários sem `BigDecimal` |

### Step 5 — Rastreamento de Fluxo de Dados
Para cada controller/endpoint:
- Quem pode chamar?
- Os parâmetros são tipados e validados (`@Valid`, Bean Validation)?
- O identificador do recurso é validado contra o proprietário da sessão (prevenção de IDOR)?
- Os dados chegam ao banco parametrizados (`PreparedStatement` / Hibernate auto-binding)?

### Step 6 — Filtro Anti-Falso-Positivo
- Aplicar as regras de `refs/false-positives.md`.
- Rebaixar ou descartar itens que não representem risco prático no contexto da arquitetura atual.
- Registrar os descartes na seção "Falsos Positivos Descartados" para transparência.

### Step 7 — Emissão e Salvamento do Relatório
- Gerar o relatório seguindo estritamente a estrutura definida em `refs/report.md`.
- Apresentar a Tabela Resumo de Severidade no topo da resposta.
- **Salvar obrigatoriamente o arquivo Markdown no projeto:**
  - Se existir `docs/`: salvar em `docs/security/audits/YYYY-MM-DD-auditoria.md` (ou com hora `YYYY-MM-DD_HHmm-auditoria.md` se houver mais de um no dia).
  - Caso contrário: salvar em `.agents/skills/security-audit/reports/YYYY-MM-DD-auditoria.md`.

### Step 8 — Propostas de Correção (Patches)
- Para cada achado classificado como **CRITICAL** ou **HIGH**:
  - Apresentar o trecho `before / after`.
  - Incluir explicitamente o aviso:
    > *"Revise cada patch antes de aplicar. Nada foi alterado no repositório."*

---

## 🚦 Tabela de Severidades

| Nível | Critério | Exemplos |
| :--- | :--- | :--- |
| **CRITICAL** | Exploração direta sem restrições ou impacto catastrófico imediato | SQL Injection direto, bypass completo de autenticação, chaves de produção ativas no Git. |
| **HIGH** | Falha de autorização com impacto relevante | IDOR permitindo alterar/excluir dados de outro usuário, endpoints com mutação sem autorização. |
| **MEDIUM** | Vulnerabilidade que exige condições específicas ou encadeamento | CORS permissivo (`*`) em endpoints autenticados, Actuator com `/env` ou `/beans` aberto. |
| **LOW** | Boas práticas de higienização e defesa em profundidade | Falta de rate limiting, headers de segurança adicionais recomendados. |
| **INFO** | Sugestões de modernização sem vulnerabilidade comprovada | Versão da dependência atrás da mais recente sem CVE ativo, recomendações de observabilidade. |

---

## 📚 Arquivos de Referência (`refs/`)

* [`refs/project-stack.md`](file:///C:/Projetos/concessionaria/.agents/skills/security-audit/refs/project-stack.md) — Detecção de versões e arquitetura (Spring Boot, Java, Maven, Frontend).
* [`refs/spring-boot-checklist.md`](file:///C:/Projetos/concessionaria/.agents/skills/security-audit/refs/spring-boot-checklist.md) — Guia de auditoria específico para APIs Spring Boot 3.
* [`refs/react-checklist.md`](file:///C:/Projetos/concessionaria/.agents/skills/security-audit/refs/react-checklist.md) — Guia de auditoria para o Frontend (React/Vite).
* [`refs/secrets.md`](file:///C:/Projetos/concessionaria/.agents/skills/security-audit/refs/secrets.md) — Regras de detecção de segredos e credenciais expostas.
* [`refs/false-positives.md`](file:///C:/Projetos/concessionaria/.agents/skills/security-audit/refs/false-positives.md) — Critérios de descarte para evitar ruído.
* [`refs/report.md`](file:///C:/Projetos/concessionaria/.agents/skills/security-audit/refs/report.md) — Modelo, layout e padrão de persistência de relatórios.
