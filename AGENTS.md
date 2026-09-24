# Diretrizes do Assistente de IA — Concessionária Gomez API

Este documento estabelece as regras de desenvolvimento, padrões arquiteturais e a conexão com a base de conhecimento do **Segundo Cérebro** para agentes de IA atuando neste repositório.

---

## 1. Conexão com o Segundo Cérebro (Obsidian Vault)

A memória técnica, decisões históricas e cadernos de erros deste projeto estão centralizados no cofre do desenvolvedor:

- **Diretório Mestre do Projeto no Vault:**  
  `D:\Google Drive\Meu Drive\Pessoal\My Second Brain\Projetos\Projeto Concessionária\`
- **Guia de Arquitetura e Decisões Técnicas:**  
  `D:\Google Drive\Meu Drive\Pessoal\My Second Brain\Projetos\Projeto Concessionária\Guia de Conceitos e Arquitetura.md`
- **Repositório de Post-Mortems (Caderno de Erros):**  
  `D:\Google Drive\Meu Drive\Pessoal\My Second Brain\Projetos\Projeto Concessionária\Post-Mortem\`
- **Template Padronizado de Post-Mortem:**  
  `D:\Google Drive\Meu Drive\Pessoal\My Second Brain\Projetos\Template - Bug Post-Mortem.md`

---

## 2. Padrões de Arquitetura e Código

1. **Stack Tecnológica:** Java 21, Spring Boot 3.x, Spring Data JPA, Flyway Migrations, Docker Compose e Maven.
2. **Separação Rígida de Camadas:**
   - **`api.controller`:** Apenas recebe requisições HTTP, coordena serviços e devolve status HTTP. Nunca contém regras de negócio ou cálculos financeiros.
   - **`domain.model` & `domain.service` (Rich Domain):** Regras de negócio e cálculos (como margem de lucro e preços sugeridos) pertencem estritamente às entidades do domínio (`Carro.java`), nunca a DTOs.
   - **`api.assembler`:** Conversão bidirecional entre entidades de banco e DTOs de apresentação via `ModelMapper` ou montadores dedicados (`CarroModelAssembler` / `CarroInputDisassembler`).
3. **Padrão RESTful:**
   - Filtros de busca e paginação devem ser implementados exclusivamente como `@RequestParam` na rota base da coleção (ex.: `/carros?ano=2020`), nunca criando sub-rotas estáticas que colidam com variáveis de caminho (`/{carroId}`).
4. **Persistência e Migrations:**
   - O esquema do banco é controlado pelo **Flyway** (`src/main/resources/db/migration/`). Nunca altere tabelas diretamente ou confie em `ddl-auto=update` em produção.

---

## 3. Protocolo de Investigação de Bugs e Post-Mortem

1. **Consulta Prévia Obrigatória:** Antes de propor soluções para incidentes de concorrência, falhas de migrations ou comportamentos atípicos de controllers, o assistente deve verificar os arquivos existentes em `D:\...\Projetos\Projeto Concessionária\Post-Mortem\` para identificar soluções e causas raízes já conhecidas.
2. **Critério de Registro de Post-Mortem:**
   - Erros triviais do dia a dia (digitação, importações faltantes, ajustes pontuais) **não devem** gerar notas.
   - Somente gere uma nova nota em `Post-Mortem/` para incidentes complexos, comportamentos em que a IA inicialmente falhou/alucinou, ou quando expressamente solicitado pelo desenvolvedor.

---

## 4. Política de Versionamento Git

- O assistente **nunca deve executar `git commit` ou `git push`** de forma autônoma.
- Todas as alterações de código devem permanecer no *working tree* para inspeção e aprovação humana prévia.
