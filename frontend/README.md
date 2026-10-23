# Gomez — protótipo do painel de estoque

Frontend local em React + TypeScript + Vite, Tailwind CSS e componentes locais no padrão shadcn/ui (Radix). Pode ser editado no IntelliJ e no Codex. Não precisa iniciar Java, MySQL ou Docker.

## Executar

Pré-requisito: Node.js 22.12+ (validado com Node 24) e npm.

No terminal do IntelliJ, a partir da raiz do repositório:

```powershell
cd frontend
npm ci
npm run dev
```

Abra **http://127.0.0.1:5173**. Ao salvar os arquivos, o Vite atualiza a interface. Se a porta estiver ocupada, encerre o processo anterior ou altere `server.port` em `vite.config.ts`.

```powershell
npm run build   # TypeScript e build de produção
npm test        # Busca, combinação de filtros e leitura de dados locais
npm run format # Formata o código para facilitar a edição
npm run preview # Abre o build de produção localmente após o build
```

## O que experimentar

- Busca por marca, modelo ou ano, com normalização de acentos.
- Filtros combinados por marca, status, ano, valor de compra e quilometragem.
- Ordenação, paginação, lista e cards.
- Cadastro e edição com validação, incluindo valores com vírgula decimal.
- Ativação e inativação, exclusão com confirmação e restauração dos exemplos.
- Visão geral com distribuição por marca e indicadores de todo o estoque.
- Layout responsivo, navegação móvel e formulários com gerenciamento de foco e Escape.

Os dados são fictícios, inspirados na carga Flyway. Alterações persistem no `localStorage` deste navegador e desta origem, sob a chave `gomez:estoque-demo:v1`. Não envie informações reais ou sensíveis para este protótipo. O botão “Restaurar dados de exemplo” limpa as alterações da demonstração após confirmação.

## Onde editar

| Arquivo | Responsabilidade |
|---|---|
| `src/App.tsx` | Navegação, indicadores, filtros, tabela e cards |
| `src/styles.css` | Paleta, tipografia, layout e responsividade |
| `src/components/VehicleForm.tsx` | Cadastro, edição e validação |
| `src/components/ui/` | Botões e janelas editáveis, no padrão shadcn/ui |
| `src/components/CarIllustration.tsx` | Ilustração SVG genérica dos veículos |
| `src/data/seed.ts` | Veículos de demonstração |
| `src/lib/estoque.ts` | Busca, filtros, ordenação, formatação e validação do armazenamento |
| `src/types.ts` | Contratos TypeScript do protótipo |

## Integração futura com Spring Boot

Este protótipo não faz chamadas HTTP. Na implementação da integração:

1. Substituir a fonte de dados local por uma camada de serviços HTTP para `/carros`.
2. Enviar os filtros como query params e usar paginação e ordenação retornadas pela API, sem filtrar só a página carregada.
3. Mapear cadastro/edição para POST/PUT e ativação/inativação para PUT/DELETE de `/{carroId}/ativo`.
4. Consumir `venda` e `zeroKm` do backend. A estimativa visual de 10% é apenas demonstrativa; os cálculos oficiais permanecem na entidade Java `Carro`.
5. Obter indicadores agregados do backend, considerando todo o estoque. Não somar apenas a página atual.
6. Apresentar os erros de validação da API e definir acesso/autenticação antes de usar dados reais.

As ilustrações não são fotos dos modelos. Clientes, vendas e fotos reais aparecem como evolução futura, de acordo com o roadmap. O backend e as migrations não foram modificados.
