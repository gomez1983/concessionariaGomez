# Checklist de Auditoria — Frontend (React / Vite / TypeScript)

Utilize este checklist quando houver código de interface web SPA / SSR no projeto (conforme previsto na Fase 5 do ROADMAP).

---

## 1. Cross-Site Scripting (XSS)

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Injeção de HTML não sanitizado** | `dangerouslySetInnerHTML={{ __html: dadoExterno }}` sem DOMPurify | **HIGH** |
| **Injeção em atributos URL (`javascript:`)** | `<a href={urlFornecidaPeloUsuario}>` sem validação de protocolo `http/https` | **HIGH** |
| **Manipulação direta de DOM** | `element.innerHTML = ...` ou `document.write` dentro de `useEffect` | **HIGH** |

---

## 2. Exposição de Segredos e Variáveis de Ambiente

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Chaves privadas em variáveis públicas** | Prefixo `VITE_API_KEY_SECRET` ou `NEXT_PUBLIC_SECRET` contendo tokens mestres | **CRITICAL** |
| **Hardcoded credentials no bundle** | Tokens de autenticação, senhas de banco ou chaves de webhook no código TSX/JSX | **CRITICAL** |

> **Nota:** `VITE_API_URL` apontando para o backend (ex: `http://localhost:8080/api`) é configuração pública legítima e **não** representa vazamento de credencial.

---

## 3. Armazenamento e Transporte de Sessão

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Tokens JWT em LocalStorage** | Armazenar tokens sensíveis em `localStorage` (vulneráveis a roubo via XSS) em vez de cookies `HttpOnly` com flag `SameSite` | **MEDIUM** |
| **Comunicação sem HTTPS em produção** | Requisições HTTP em texto claro para endpoints que trafegam credenciais | **HIGH** |

---

## 4. Validação de Formulários & Tratamento de Erros da API

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Envio cego de dados sem validação client-side** | Ausência de schemas (Zod/Yup) permitindo payloads inválidos antes do envio | **LOW** |
| **Exibição direta de erros técnicos ao usuário final** | Exibir JSON cru de erro ou stack trace do backend na interface | **LOW** |
