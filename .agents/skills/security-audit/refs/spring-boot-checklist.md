# Checklist de Auditoria — Spring Boot 3 & REST APIs

Utilize este checklist ao auditar controladores, serviços, repositórios e configurações no ecossistema Spring Boot.

---

## 1. Injeção de Código e SQL / JPQL

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Concatenação em `@Query`** | `@Query("SELECT c FROM Carro c WHERE c.modelo = '" + param + "'")` | **CRITICAL** |
| **Native Queries inseguras** | `@Query(value = "SELECT * FROM ... " + filtro, nativeQuery = true)` | **CRITICAL** |
| **EntityManager dinâmico** | `entityManager.createQuery("... " + input)` ou `createNativeQuery` com interpolação de strings | **CRITICAL** |
| **SpEL Injection** | Avaliação dinâmica de expressões Spring Expression Language com input de usuário | **HIGH** |

> **Nota:** Queries declaradas como derivadas (`findByModeloContaining`, `findByAno`) ou com parâmetros nomeados (`:modelo`) via Hibernate/JPA usam `PreparedStatement` automaticamente e **não** são vulneráveis a SQLi.

---

## 2. Mass Assignment & Integridade de DTOs (Over-Posting)

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Entidade JPA como `@RequestBody`** | `@PostMapping public Carro salvar(@RequestBody Carro carro)` | **HIGH** |
| **ID no payload de criação/edição** | DTO de entrada contendo campo `id` que sobrescreve o identificador gerenciado | **HIGH** |
| **Mutação de campos imutáveis** | Atualização em lote que sobrescreve `dataCadastro`, `dataCompra` ou status de negócio sem regra | **MEDIUM** |
| **Mapeamento permissivo** | `ModelMapper` ou similar mapeando propriedades nulas ou campos protegidos da entidade | **MEDIUM** |

> **Boa prática comprovada:**
> - Usar DTOs dedicados de entrada (ex: `CarroInput`) e de saída (ex: `CarroModel`).
> - Configurar o disassembler para ignorar `id` e campos de auditoria durante o binding no objeto de domínio.

---

## 3. Autorização e Controle de Acesso (IDOR / BOLA)

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Exclusão/Alteração arbitrária por ID** | `@DeleteMapping("/{id}")` sem checagem de permissão ou contexto do operador | **HIGH** |
| **Ativação/Inativação irrestrita** | Endpoints de mutação de estado (`PUT /{id}/ativo`) sem controle de papel/perfil | **HIGH** |
| **Exposição de recursos privados** | Endpoints que retornam dados sensíveis de clientes ou relatórios para qualquer requisitante | **HIGH** |

---

## 4. Vazamento de Informações & Tratamento de Erros

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Exposição de Stack Trace** | `ApiExceptionHandler` retornando o `e.getMessage()` cru do banco de dados | **MEDIUM** |
| **Nomes de tabelas e constraints na resposta** | Retornar detalhes de `DataIntegrityViolationException` com SQL interno | **MEDIUM** |
| **Logs expondo dados sensíveis** | `log.info("Processando: {}", payloadComSenhaOuCpf)` | **MEDIUM** |
| **RFC 7807 (Problem Details)** | Respostas de erro sem padronização ou com campos não sanitizados | **LOW** |

---

## 5. Validação de Dados de Entrada & Tipagem Financeira

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **Falta de `@Valid`** | `@RequestBody CarroInput input` sem anotação `@Valid` | **HIGH** |
| **Campos numéricos sem restrição** | Campos como `ano` ou `preco` aceitando valores negativos sem `@Positive` / `@Min` | **LOW** |
| **Tipos primitivos em valores monetários** | Uso de `double` ou `float` para compra/venda (risco de imprecisão contábil) | **MEDIUM** |

---

## 6. Configurações de Segurança e Infraestrutura

| Verificação | O que procurar | Risco |
| :--- | :--- | :--- |
| **CORS totalmente aberto com credenciais** | `@CrossOrigin("*")` associado a autenticação por cookie/sessão | **MEDIUM** |
| **Actuator exposto irrestritamente** | `management.endpoints.web.exposure.include=*` aberto à internet | **HIGH** |
| **Documentação Swagger aberta em Prod** | `/swagger-ui/index.html` exposto publicamente sem proteção em ambiente produtivo | **INFO / LOW** |
