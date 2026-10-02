# 🛡️ Skill: Security Audit (Spring Boot & React)

Skill adaptada para o projeto **Concessionária**, desenhada para realizar auditorias estritas de segurança em APIs **Spring Boot 3 (Java 21)** e aplicações frontend **React/TypeScript**.

A skill opera sob a ótica de um **pesquisador de segurança**: prioriza contexto de negócio, validação do fluxo completo de dados e defesas nativas do framework, com forte filtro anti-falso-positivo.

---

## 📂 Estrutura da Skill no Repositório

```text
.agents/skills/security-audit/
├── SKILL.md                          # Instruções principais e gatilhos da skill
├── README.md                         # Documentação de uso e visão geral
└── refs/                             # Guias e checklists especializados
    ├── project-stack.md              # Detecção de versões e arquitetura (pom.xml / package.json)
    ├── spring-boot-checklist.md      # Regras para Spring Boot 3, JPA, DTOs e APIs REST
    ├── react-checklist.md            # Regras para Frontend React / Vite (Fase 5 do ROADMAP)
    ├── secrets.md                    # Varredura de credenciais e segredos em arquivos de config
    ├── false-positives.md            # Filtro de descarte para evitar alarmes falsos
    └── report.md                     # Template padronizado do relatório de auditoria
```

---

## 🚀 Como Executar

Você pode acionar a auditoria conversando com o assistente Antigravity / Cursor:

### Auditoria Geral do Projeto
```text
Roda a skill security-audit neste projeto
```
ou simplesmente:
```text
/security-audit
```

### Auditoria em Escopo Específico
```text
/security-audit src/main/java/com/algaworks/concessionaria/api
```

---

## 🔍 O que a Skill Analisa

1. **Injeção de Código & SQL:**
   - Varredura de consultas customizadas no Spring Data JPA / Hibernate contra interpolação insegura de strings.
2. **Mass Assignment & Integridade de DTOs:**
   - Blindagem de endpoints `POST` e `PUT` para evitar que identificadores (`id`) ou campos do sistema (`dataCompra`, etc.) sejam manipulados indevidamente por payloads externos.
3. **Controle de Acesso & IDOR:**
   - Verificação de mutações em recursos por identificador (`/carros/{id}`, `/carros/{id}/ativo`).
4. **Vazamento de Dados & Exception Handling:**
   - Inspeção de handlers de exceção (`ApiExceptionHandler`) para garantir que stack traces e erros de banco de dados não sejam expostos ao cliente (RFC 7807).
5. **Segredos & Dependências:**
   - Verificação de senhas em arquivos de configuração e checagem de CVEs reais nas dependências do `pom.xml`.
6. **Frontend (quando presente):**
   - Proteção contra XSS, sanitização de inputs e vazamento de chaves privadas em bundles web.

---

## 🛡️ Garantias da Auditoria

* **Sem Alterações Silenciosas:** Todas as correções são geradas no formato diff (`before/after`) como sugestão para revisão prévia. Nenhum código de produção é alterado automaticamente.
* **Sem Ruído:** Consultas derivadas do Spring Data JPA, testes de integração (`CadastroCarroIT`) e o Swagger UI local não são taxados como falhas críticas.
