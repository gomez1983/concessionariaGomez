# Padrão e Estrutura do Relatório de Auditoria de Segurança

## 📁 Regra de Persistência e Padrão de Nomenclatura

Toda vez que a auditoria for executada, além de apresentar o resultado na conversa, o relatório completo **DEVE ser salvo em arquivo Markdown** para fins de histórico e conformidade:

1. **Diretório Padrão:**
   - Se existir pasta `docs/` no projeto: salvar em `docs/security/audits/`
   - Caso contrário: salvar em `.agents/skills/security-audit/reports/`
   *(Se a pasta não existir, deve ser criada com `New-Item -ItemType Directory -Force`)*

2. **Nomenclatura do Arquivo:**
   - Padrão diário: `YYYY-MM-DD-auditoria.md` (exemplo: `2026-10-02-auditoria.md`)
   - Em caso de múltiplas auditorias no mesmo dia: `YYYY-MM-DD_HHmm-auditoria.md`

---

## 📑 Modelo do Relatório

Todo relatório gerado deve seguir rigorosamente a estrutura abaixo:

```markdown
# 🛡️ Relatório de Auditoria de Segurança — [Nome do Projeto]

**Data da Auditoria:** [DD/MM/AAAA - HH:MM]  
**Escopo Auditado:** `[Caminho analisado, ex: raiz do projeto ou src/main/java]`  
**Arquivo do Relatório:** `docs/security/audits/YYYY-MM-DD-auditoria.md`  

---

### 🔍 Stack Detectado
- **Backend:** Java [versão], Spring Boot [versão] (Maven)
- **Persistência & Banco:** Spring Data JPA / Hibernate [versão], [Banco de dados], [Flyway/Liquibase]
- **Documentação de API:** SpringDoc OpenAPI [versão]
- **Frontend:** [React Vite / Next.js / Não detectado no escopo]
- **Mecanismos de Segurança:** [Spring Security / Controles em Controller]

---

## 📊 1. Resumo Executivo de Severidade

| Severidade | Quantidade | Status Principal |
| :--- | :---: | :--- |
| 🔴 **CRITICAL** | 0 | Ação imediata requerida |
| 🟠 **HIGH** | 0 | Risco alto de exploração |
| 🟡 **MEDIUM** | 0 | Correção recomendada no ciclo atual |
| 🔵 **LOW** | 0 | Melhoria de defesa em profundidade |
| ⚪ **INFO** | 0 | Observações arquiteturais / Boas práticas |

---

## 🔎 2. Detalhamento dos Achados por Categoria

### [Nome da Categoria, ex: Controle de Acesso / Mass Assignment / Validação]

#### [SEC-01] [Título claro da vulnerabilidade]
- **Severidade:** `[CRITICAL | HIGH | MEDIUM | LOW | INFO]`
- **Localização:** `[caminho/do/Arquivo.java:linha]`
- **Evidência:**
```java
// Trecho de código relevante extraído do arquivo
```
- **Impacto:** Explicação concisa do risco real de negócio ou exploração técnica.
- **Recomendação:** Ação técnica para mitigar o problema.

---

## 🛠️ 3. Propostas de Correção (Patches Sugeridos)

> ⚠️ **Revise cada patch antes de aplicar. Nada foi alterado no repositório.**

```diff
--- a/src/main/java/com/concessionaria/gomez/api/controller/CarroController.java
+++ b/src/main/java/com/concessionaria/gomez/api/controller/CarroController.java
@@ -30,6 +30,7 @@
-    public CarroModel atualizar(@PathVariable Long id, @RequestBody CarroInput input) {
+    public CarroModel atualizar(@PathVariable Long id, @RequestBody @Valid CarroInput input) {
```

---

## 📦 4. Análise de Dependências e CVEs
- **Status do Maven/Lockfile:** [Nenhuma vulnerabilidade crítica conhecida nas versões declaradas / Detalhes de CVEs identificadas]

---

## 🔑 5. Varredura de Segredos e Arquivos Rastreados
- **Arquivos sensíveis rastreados no Git:** [Nenhum arquivo sensível detectado / Lista de arquivos]
- **Credenciais em arquivos de configuração:** [Status de senhas em application.properties e docker-compose]

---

## 🧹 6. Falsos Positivos Descartados
Lista de elementos analisados que foram validados e desconsiderados para evitar ruído:
- *Exemplo: Métodos de busca no repositório utilizam queries derivadas seguras contra SQLi.*
- *Exemplo: Swagger UI habilitado intencionalmente para ambiente de desenvolvimento local.*

---

## 🎯 7. Conclusão e Próximos Passos
Recomendações finais priorizadas para a equipe técnica.
```
