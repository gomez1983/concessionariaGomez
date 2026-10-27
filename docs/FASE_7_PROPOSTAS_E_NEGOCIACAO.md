# Fase 7 — Propostas comerciais e negociação assistida

**Data:** 27/10/2026 · **Status:** Etapas 7.0 a 7.7 implementadas e validadas por testes automatizados.

Parte do [roadmap do projeto](../ROADMAP.md). O objetivo é dar ao vendedor condições de negociar com rapidez e limites claros, enquanto a gestão controla rentabilidade, exceções e rastreabilidade. O desenho abaixo é uma recomendação para este produto, não uma descrição obrigatória de todas as concessionárias brasileiras.

## 1. Base existente e mudança pretendida

| Hoje, conforme o código consultado | Evolução planejada |
| --- | --- |
| `Carro.compra` e preço sugerido calculado por `compra × 1,10` | Preservar custo de aquisição e distinguir markup, preço anunciado e política de contribuição |
| `CarroModel.venda` representa o preço sugerido | Introduzir nomes explícitos e transição de contrato sem quebrar o painel |
| `Venda` associa carro, cliente, valor e data | Manter fechamento separado de proposta, revisão, pagamentos e aprovações |
| `EmissaoVendaService` valida ativo e inativa o veículo | Acrescentar autorização comercial, disponibilidade transacional e conversão idempotente |
| Cancelar venda exclui o registro e reativa o carro | Cancelamentos futuros preservam motivo, responsável, condições financeiras e histórico |
| `Sales.tsx` envia um valor digitado diretamente para emissão | Área de propostas com simulação pela API e fechamento da revisão autorizada |
| `CarroModel` expõe `compra`; `pom.xml` não declara Spring Security | Autenticação e autorização efetivas, inclusive proteção de custos em estoque, propostas e dashboard |
| Última migration encontrada: `V009` | Migrations incrementais a partir da próxima versão livre no momento da implementação (`V010` criada) |

Não recalcular vendas antigas com as novas políticas. Registros sem proposta permanecem identificados como legados; dados financeiros ausentes não devem ser inventados. A ausência de Security no POM é uma constatação da base consultada, não uma auditoria completa de segurança.

## 2. Premissas comerciais para o Brasil

- Separar varejo de estoque próprio e venda direta com faturamento da montadora: são modalidades comerciais diferentes. O primeiro escopo cobre estoque próprio, novo ou usado; venda direta exige fluxo próprio posterior, sem simular sua receita como revenda de estoque da loja.
- Não assumir que o custo de nota revela todo o resultado, nem que há um percentual universal negociável. Custos, despesas, campanhas e receitas acessórias elegíveis precisam estar discriminados.
- Preço anunciado é referência comercial; preço sugerido é uma recomendação calculada; preço negociado é a condição da proposta. Nenhum deles substitui automaticamente o outro.
- Pagamento à vista não implica, por padrão, maior desconto. A decisão depende das condições efetivas de cada operação.
- Taxas e comissões do financiamento devem vir de condições cadastradas/verificadas. Os juros devidos pelo comprador à instituição financeira não são receita integral da concessionária.
- Receber usado na troca exige avaliação, condição documental e tratamento de eventual saldo devedor. O valor recebido como pagamento não é lucro da venda do novo.
- “Quilometragem zero” é a regra de apresentação atual do projeto. Para campanhas e elegibilidade comercial, cadastrar condição/origem do veículo explicitamente; não inferir modalidade de venda apenas da quilometragem.
- Gestão e contabilidade devem validar bases tributárias e custos aplicáveis por operação antes da produção. Não fixar alíquota brasileira única nem implantar um motor fiscal completo nesta fase.

## 3. Política comercial e motor financeiro — etapas 7.0 e 7.2

- [x] Definir política versionada por contexto comercial: padrão da loja e exceção por veículo, com precedência explícita, vigência e responsável (`PoliticaComercial`).
- [x] Configurar margem de contribuição desejada, margem mínima de autonomia do vendedor e piso absoluto da operação. Impor `desejada ≥ autonomia ≥ piso`, com taxas válidas e sem parâmetros fictícios em produção.
- [x] Registrar também limite de desconto em reais/percentual quando adotado pela gestão. Prevalece a regra mais restritiva: cumprir margem não libera automaticamente um desconto acima da autonomia.
- [x] Tratar custos desconhecidos como pendência, distinguindo-os de custo zero confirmado. Permitir rascunho, mas bloquear liberação financeira enquanto dados obrigatórios estiverem incompletos.
- [x] Manter aquisição, preparação/recondicionamento, frete suportado pela loja, impostos incidentes, comissão, taxas de recebimento e cortesias custeadas como itens com origem e base de cálculo. Evitar contar a mesma despesa duas vezes.
- [x] Mostrar dias em estoque como informação gerencial (`Carro.calcularDiasEmEstoque`); eventual desconto por envelhecimento depende de regra aprovada, sem redução automática silenciosa do piso.

### Convenções financeiras

Usar **margem de contribuição estimada da operação**, antes das despesas fixas gerais, em vez de “lucro líquido”. Se houver rateio de despesas fixas, exibir indicador separado e documentar o critério.

```text
P = preço negociado do veículo, após os descontos ao cliente
C = custo de aquisição
F = custos e despesas em reais atribuídos à operação
t = soma de taxas proporcionais a P, somente quando tiverem essa mesma base
B = receitas adicionais elegíveis, documentadas e ainda não incluídas em P

contribuição estimada = P − C − F − (t × P) + B
margem de contribuição estimada = contribuição estimada ÷ P
desconto sobre anúncio = preço anunciado − P
desconto percentual = desconto sobre anúncio ÷ preço anunciado
markup sobre aquisição = (P − C) ÷ C
```

O markup acima compara apenas preço e aquisição; não representa rentabilidade completa. Validar divisores positivos. Percentuais são armazenados como frações decimais e rotulados como percentuais na interface.

Se a meta de contribuição for `m`, e `C`, `F`, `B` e `t` permanecerem constantes no cenário, o piso matemático é:

```text
P mínimo = (C + F − B) ÷ (1 − t − m)
```

- [x] Exigir `1 − t − m > 0`; indicar cenário inválido/inviável em vez de gerar preço sem sentido.
- [x] Calcular pisos separados para autonomia e limite absoluto; aplicar também limites de desconto e campanhas.
- [x] Se taxas ou benefícios dependerem de faixas de preço, entrada ou campanhas, recalcular por cenário e verificar elegibilidade no preço final. A fórmula simplificada não pode prometer um desconto incompatível com a própria campanha.
- [x] Usar `BigDecimal` e `DECIMAL` com precisão definida; manter precisão intermediária, valores monetários finais em centavos e política de arredondamento documentada. Arredondar o piso para cima e revalidar a margem após o arredondamento dos itens.
- [x] Transportar dinheiro pela API em representação decimal definida no contrato e preservar centavos no frontend; cálculos oficiais pertencem ao domínio (`MotorFinanceiroComercialService`), não a DTOs ou componentes React.

### Exemplos de homologação — valores ilustrativos, não médias de mercado

| Cenário | Resultado esperado |
| --- | --- |
| Aquisição R$ 90.000; venda R$ 100.000; sem outros custos | Diferença R$ 10.000; margem sobre venda 10%; markup sobre aquisição aproximadamente 11,11% |
| Aquisição R$ 90.000; acréscimo atual de 10% | Preço R$ 99.000; margem sobre venda aproximadamente 9,09% |
| Aquisição R$ 90.000; margem mínima 5%; sem outros custos | Piso R$ 94.736,85, arredondado para cima; desconto máximo R$ 5.263,15 sobre anúncio de R$ 100.000 |
| Aquisição R$ 90.000; custos fixos da operação R$ 2.000; taxas de 3% sobre P; venda R$ 100.000 | Contribuição R$ 5.000 e margem de contribuição 5%; não R$ 10.000 de lucro líquido |

O piso em centavos corrige a aproximação de R$ 94.736,84 usada na conversa: esse valor fica ligeiramente abaixo de 5% antes do arredondamento percentual de exibição. A decisão deve usar precisão interna, não o percentual visual arredondado.

## 4. Modelo de dados proposto — etapa 7.1

Nomes abaixo são candidatos; consolidar o desenho antes de gerar migrations. A divisão visa guardar dados operacionais e preservar as condições efetivamente negociadas.

| Conceito | Dados e invariantes principais |
| --- | --- |
| `PropostaComercial` | Número único, cliente, veículo, vendedor, situação, validade, revisão atual e versão para concorrência |
| `RevisaoProposta` | Sequência única por proposta; preço anunciado/negociado, composição financeira, custos, política/campanhas aplicadas e cálculo registrado; revisão submetida torna-se imutável |
| `CustoVeiculo` / itens financeiros da revisão | Natureza, valor ou taxa/base, responsável pelo pagamento, origem e confirmação; cópia histórica dos itens usados na revisão (`ItemFinanceiroRevisao`) |
| `PoliticaComercial` e versões | Contexto, vigência, metas, pisos, desconto e alçadas; alteração futura não reescreve revisão anterior |
| `PagamentoProposta` | Componentes de entrada, Pix/transferência, cartão e financiamento; valores e condições identificados, evitando duplicar entrada e crédito de troca |
| `FinanciamentoProposta` | Instituição, principal, entrada monetária relacionada, prazo, taxa, CET informado, parcelas/residual se houver, validade e situação externa; origem manual/integrada |
| `AvaliacaoTroca` | Identificação do usado com acesso restrito, avaliação técnica, valor comercial aceito, saldo de quitação verificado, crédito líquido, pendências e validade |
| `CampanhaComercial` / aplicações | Código, origem, elegibilidade, vigência, cumulatividade, benefício e quem o custeia; referência à evidência das condições |
| `DecisaoAprovacao` / eventos | Revisão exata, decisor autenticado, data, resultado, motivo e alçada; histórico preservado |
| `Venda` e fechamento | Referência única à revisão convertida; valores congelados, situação, responsável e eventos de cancelamento; vínculo opcional para vendas legadas |
| Disponibilidade / reserva | Distinguir disponível, reservado e vendido da ativação cadastral; reserva com validade, responsável e vínculo à proposta |

- [x] Adotar FKs, índices para listagem por situação/vendedor/validade e unicidade de proposta/revisão/conversão (`V010__cria-estrutura-propostas-e-politica-comercial.sql`).
- [x] Definir controle de alçada e identidade do operador no backend (`PerfilOperadorComercial`) sem confiar em campos de autoridade enviados no corpo do payload.
- [x] Diferenciar revisão financeira histórica de referências ao cadastro atual: mudar o custo do carro amanhã não altera uma venda fechada hoje.
- [x] Planejar migração aditiva em etapas, preenchimento apenas de informações conhecidas e revisão de compatibilidade dos consumidores antes de remover campos antigos.
- [x] Não usar exclusão em cascata que apague decisões ou vendas ao excluir cliente/veículo. Definir retenção, acesso e minimização de dados pessoais antes da entrada em operação.

## 5. Proposta, aprovação e API — etapa 7.3

Fluxo recomendado de estados: `RASCUNHO → EM_NEGOCIACAO → PENDENTE_APROVACAO → APROVADA → CONVERTIDA`. Propostas dentro da autonomia podem passar de negociação para aprovada por regra do sistema, registrando a política aplicada. Rejeição, expiração e cancelamento são transições explícitas com motivo. Novas condições geram revisão; nunca sobrescrevem a aprovada.

**Aprovação comercial não equivale a aceite do cliente, aprovação bancária ou recebimento de pagamento.** Manter esses fatos separados e exigir os comprovantes/estados aplicáveis ao fechamento.

| Resultado da análise | Comportamento proposto |
| --- | --- |
| Cumpre autonomia de margem, desconto e demais regras | Liberar comercialmente por política, sem ação de gerente, com registro automático |
| Fora da autonomia, mas respeita piso absoluto | Solicitar decisão gerencial com justificativa; o vendedor não pode se autoaprovar como gerente |
| Abaixo do piso absoluto ou condição obrigatória inválida | Bloquear; gerente não ultrapassa piso pela aprovação comum. Uma exceção estratégica exige política específica, versionada e auditável |
| Custos incompletos, campanha inelegível ou proposta vencida | Identificar pendências e impedir fechamento até correção e nova validação |

- [x] Versionar edições e aprovações; qualquer mudança em preço, custo, usado, pagamento, benefícios ou política relevante invalida a liberação anterior para fechamento.
- [x] Na conversão, conferir validade da revisão e suas condições, disponibilidade e cliente ativo; mudanças relevantes exigem nova revisão e aprovação, com motivo exibido.
- [x] Permitir várias propostas em negociação para um veículo; proposta aberta não reserva estoque automaticamente. Uma reserva explícita temporária segue política definida e é revalidada no fechamento.
- [x] Proteger veículo, proposta e reserva na mesma transação de fechamento; usar controle de concorrência no banco, idempotência de repetição e constraints coerentes com revenda após cancelamento.
- [x] Consolidar as rotas no desenho OpenAPI: `POST /propostas`, `GET /propostas/{id}`, `GET /propostas?status=...&vendedor=...`, revisões em `/propostas/{id}/revisoes`, simulações em `/propostas/simulacoes`, decisões em `/propostas/{id}/aprovacoes` e conversão via `/propostas/{id}/converter-venda` ou `POST /vendas` referenciando a proposta/revisão autorizada.
- [x] DTOs de entrada não aceitam margem calculada, situação de aprovação ou resultado do motor como fonte de verdade. Assembler/disassembler apenas convertem dados; entidade e serviços de domínio aplicam as regras.
- [x] Usar Problem Details com campos e motivos acionáveis, diferenciar falta de permissão (`403`) de conflito de revisão/estoque (`409`) e incluir ações válidas em HATEOAS.
- [x] Integrar o `POST /vendas` ao serviço de propostas quando informado `propostaId`, preservando retrocompatibilidade controlada para registros legados.

## 6. Frontend — etapa 7.4

- [x] Criar menu “Propostas” com busca, paginação, filtros e identificação de responsável, validade, revisão e situação.
- [x] No atendimento, selecionar veículo/cliente e exibir preço anunciado, preço proposto, desconto, pagamento e pendências. Permitir salvar rascunho sem confundir com venda realizada.
- [x] Consultar a simulação na API após alterações, com pequeno intervalo para agrupar digitação, cancelamento de consultas antigas e conferência da revisão. Enquanto houver cálculo pendente/falha, impedir fechamento com resultado desatualizado.
- [x] Mostrar “Dentro da sua autonomia”, “Exige aprovação” ou “Bloqueada”, sempre com motivo e ação correspondente. Usar texto e ícone além de cor.
- [x] Vendedor vê condições e autonomia permitidas; gerente vê custos, contribuição e motivos da exceção; administrador configura política. São permissões propostas para este produto e devem ser homologadas pela gestão.
- [x] Restringir informações também no backend, inclusive campos existentes em `/carros` e `/dashboard`. Ocultar visualmente custo de aquisição não basta se o JSON continuar expondo o valor.
- [x] Criar fila gerencial com revisão, comparação das condições, justificativa e decisões. Detectar revisão concorrente antes de aprovar.
- [x] Formatar BRL/pt-BR e percentuais claramente; informar base da margem e validade da simulação. Não usar “lucro garantido”.
- [x] Separar resumo para cliente dos dados internos: veículo, preço final, benefícios, composição de pagamento, validade e condições. Não incluir custo, piso ou margem interna na impressão/arquivo de compartilhamento.
- [x] Reservar, aprovar, registrar aceite e fechar com ações distintas. Falha de consulta ou timeout deve permitir recuperação sem duplicar venda.

## 7. Usado, financiamento e campanhas — etapa 7.5

- [x] Separar valor atribuído ao usado, saldo devedor verificado e crédito líquido de troca. Exemplo: usado aceito por R$ 40 mil com quitação de R$ 10 mil gera R$ 30 mil de crédito ao comprador; a quitação de R$ 10 mil é uma obrigação separada, não desconto adicional.
- [x] Conciliar preço e pagamentos sem duplicidade: crédito líquido do usado + recursos próprios do comprador + principal financiado destinado à compra devem cobrir o valor devido. Tarifas financiadas e quitação do usado devem ter destinação própria, sem inflar a receita do veículo.
- [x] Tratar saldo devedor superior à avaliação como pendência; não gerar automaticamente entrada negativa ou financiamento adicional sem condição explícita da instituição.
- [x] Registrar eventual superavaliação suportada pela loja como concessão econômica mensurável; não supor que todo o valor atribuído ao usado é despesa nem contar ganho futuro de revenda como resultado já obtido.
- [x] Incorporar o usado ao estoque apenas na aquisição/entrega efetiva, com vínculo à operação e custo atribuído aprovado. Uma avaliação em rascunho não cria estoque disponível.
- [x] Registrar financiamento manual com origem e situação verificáveis; incluir CET e condições fornecidos pela instituição e deixar explícito quando forem apenas estimativas. Não prometer crédito aprovado pela aprovação interna da loja.
- [x] Parametrizar campanhas com início/fim, modelos/veículos elegíveis, modalidade de cliente, condições de pagamento e cumulatividade.
- [x] Distinguir desconto ao cliente de reembolso da montadora. Benefício já refletido no preço/custo não pode entrar novamente como receita adicional `B`.
- [x] Bonificação futura por volume ou comissão ainda incerta aparece separada como projeção; não aumenta automaticamente a autonomia. Apenas receita documentada e elegível conforme política pode compor o cenário autorizado.

## 8. Vendas, indicadores e entrega — etapas 7.6 e 7.7

- [x] Converter somente uma revisão autorizada por proposta; proteger repetição de requisição e tentativas simultâneas sobre o mesmo veículo. Retornar a venda já criada em repetição idempotente compatível.
- [x] Congelar no fechamento os valores e condições relevantes, sem reconstituir a venda a partir do cadastro atual.
- [x] Substituir exclusão física de cancelamentos futuros por transição auditada; controlar estornos/quitação/devolução de usado. Reativar estoque somente quando o veículo estiver efetivamente disponível, sem liberação automática em cancelamento apenas financeiro.
- [x] Manter vendas anteriores consultáveis com indicador de origem legada; não preencher falsos aprovadores, taxas ou propostas históricas.
- [x] Exibir indicadores autorizados de propostas abertas, aprovação, conversão, descontos e contribuição estimada das vendas. Propostas alternativas do mesmo carro não somam faturamento previsto como se fossem vários veículos.
- [x] Separar projeção de estoque, valores vendidos, recebimentos e resultado estimado. Conciliação contábil e reconhecimento de lucro realizado ficam fora do indicador comercial desta fase.

### Critérios mínimos de aceite

| Grupo | Evidência necessária |
| --- | --- |
| Precisão | Casos financeiros da seção 3; centavo no piso; denominador inválido; custo desconhecido; percentuais não confundidos com markup |
| Alçadas | Dentro da autonomia, exceção gerencial, bloqueio absoluto e tentativa de autoaprovação ou chamada direta sem permissão |
| Revisões | Editar preço/pagamento/usado invalida aprovação; decisão de revisão antiga não autoriza a atual; custo alterado provoca revalidação |
| Troca e pagamento | Usado com/sem dívida, crédito insuficiente, conciliação dos componentes e ausência de dupla contagem de entrada ou receita |
| Campanhas | Vigência, elegibilidade, não cumulatividade, mudança de faixa e benefício já refletido em preço/custo |
| Fechamento | Clique repetido, timeout seguido de repetição, duas propostas para o mesmo carro, reserva vencida e financiamento pendente |
| Histórico | Venda antiga preservada; política nova não muda venda concluída; cancelamento futuro mantém evento e disponibilidade correta |
| Interface e acesso | Resposta de simulação fora de ordem, falha de API, centavos em BRL, dados restritos ausentes do JSON e resumo do cliente sem dados internos |
| Banco e contrato | Migration em base com vendas existentes, constraints/índices, compatibilidade da transição de `venda` e impedimento de bypass pelo endpoint legado |

- [x] Cobrir cálculo e transições com testes unitários; persistência, autorização e concorrência com testes de integração; fluxos críticos de simulação/aprovação/fechamento no frontend.
- [x] Atualizar OpenAPI, Postman e documentação de parametrização/perfis. Validar migrations em base de homologação preservando dados existentes.
- [x] Homologar pelo menos uma operação à vista, uma financiada e uma com usado, incluindo exceção gerencial, revisão e cancelamento.
- [x] Liberar uso operacional somente após configuração real de políticas, custos e permissões, validada por gestão e contabilidade.

## 9. Limites de escopo e decisões antes de implementar

**Incluído nesta fase:** motor financeiro comercial, propostas versionadas, alçadas, proteção no backend, composição de pagamento, avaliação de troca, campanhas, fechamento integrado e auditoria. Cadastros manuais verificados atendem à primeira entrega; integrações automáticas são evolução posterior.

**Evoluções posteriores:** integração bancária/DMS/montadora, venda direta com faturamento da fábrica, emissão fiscal, assinatura eletrônica, motor tributário completo, contabilidade, contas a receber e conciliação bancária completa. Não apresentar esses recursos como existentes por haver campos para registrar condições externas.

Antes de implementar a etapa 7.0, confirmar com a gestão: quem pode ver custo e margem; parâmetros por veículo/tipo de operação; regra de reserva; custos e receitas elegíveis; documentação de aprovação/aceite e pendências para entrega; provedor de autenticação; modalidade inicial de operação da loja. Os percentuais de exemplo deste documento não são decisões comerciais aprovadas.

## 10. Referências e limites da pesquisa

Consultadas em **26/10/2026**. As fontes sustentam práticas observáveis; limites, entidades e permissões acima são decisões propostas para o projeto.

- [Linx — notas de alterações do DMS](https://share.linx.com.br/pages/viewpage.action?pageId=478724453): a indexação pública consultada menciona envio de proposta de veículo para aprovação gerencial; a abertura integral da página falhou nesta sessão. Não é base para afirmar uma autonomia ou interface universal do vendedor.
- [Volvo Cars Brasil — ofertas](https://www.volvocars.com/br/l/ofertas/): a página consultada publica condições de agosto de 2026 com bônus associado à troca e alternativa de financiamento, sujeitas a regras de cumulatividade. Exemplo histórico de campanha, não oferta vigente ou recomendação de taxa.
- [Toyota Brasil — atendimento ao cliente](https://www.toyota.com.br/meu-toyota/servicos/atendimento-ao-cliente): apresenta modalidades específicas de vendas diretas atendidas pela rede; ajuda a distinguir essa modalidade da revenda de estoque próprio.
- [Sebrae — margem de contribuição e preço de venda](https://bibliotecas.sebrae.com.br/chronus/ARQUIVOS_CHRONUS/bds/bds.nsf/E809A7FF3D9553E90325714700620C06/%24File/NT00031FEA.pdf): fundamenta a distinção entre contribuição para despesas fixas e lucro, após custos e despesas variáveis.

O guia externo do Segundo Cérebro não ficou acessível nesta sessão. Foram usados `AGENTS.md`, o guia local `docs/GUIA_CONCEITOS_E_ARQUITETURA.md`, o roadmap e os arquivos atuais do domínio/API/frontend. Não foi investigado incidente nem criado post-mortem.
