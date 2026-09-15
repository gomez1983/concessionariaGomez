# Changelog

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

---

## [0.1.0-BETA] - 2026-09-14

### Adicionado
- **Migração Financeira para `BigDecimal`:**
  - Substituição de `Double`/`double` por `BigDecimal` nas entidades e DTOs (`Carro`, `CarroInput`, `CarroModel`).
  - Cálculo de margem de venda com precisão decimal (`RoundingMode.HALF_UP`).
  - Migration Flyway `V004__altera-compra-para-decimal.sql` convertendo o campo `compra` para `decimal(10, 2) not null`.
- **Suporte a Validação:** Anotação `@PositiveOrZero` no campo `compra` em `CarroInput`.
- **Documentação:** Criação do `ROADMAP.md` e `CHANGELOG.md`.

### Corrigido
- **Conflito de Porta MySQL:** Remapeamento da porta host do MySQL no Docker de `3306` para `3307` em `docker-compose.yml`, `application.properties` e `application-test.properties` para evitar conflito com serviço local do Windows.
- **Suíte de Testes de Integração:** Atualização da base de testes REST-Assured (`CadastroCarroIT`) para utilizar a porta `3307` e objetos `BigDecimal`.
