package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.AcessoComercialNegadoException;
import com.concessionaria.gomez.domain.exception.ConflitoNegociacaoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.exception.PropostaNaoEncontradaException;
import com.concessionaria.gomez.domain.model.*;
import com.concessionaria.gomez.domain.repository.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestaoPropostaComercialService {

    private final PropostaComercialRepository propostaRepository;
    private final RevisaoPropostaRepository revisaoRepository;
    private final PoliticaComercialRepository politicaRepository;
    private final CustoVeiculoRepository custoVeiculoRepository;
    private final CampanhaComercialRepository campanhaRepository;
    private final VendaRepository vendaRepository;
    private final CadastroCarroService cadastroCarroService;
    private final CadastroClienteService cadastroClienteService;
    private final MotorFinanceiroComercialService motorFinanceiro;

    @Getter
    @Builder
    public static class DadosRevisaoCommand {
        private final Long carroId;
        private final Long clienteId;
        private final String vendedorIdentificador;
        private final BigDecimal precoNegociado;
        private final String justificativaVendedor;
        private final boolean submeter;
        @Builder.Default
        private final List<ItemFinanceiroRevisao> itensExtras = Collections.emptyList();
        @Builder.Default
        private final List<PagamentoProposta> pagamentos = Collections.emptyList();
        private final FinanciamentoProposta financiamento;
        private final AvaliacaoTroca avaliacaoTroca;
        @Builder.Default
        private final List<String> codigosCampanha = Collections.emptyList();
    }

    public PropostaComercial buscarOuFalhar(Long propostaId) {
        return propostaRepository.findById(propostaId)
                .orElseThrow(() -> new PropostaNaoEncontradaException(propostaId));
    }

    public RevisaoProposta buscarRevisaoAtual(PropostaComercial proposta) {
        return revisaoRepository.findByPropostaIdAndSequencia(proposta.getId(), proposta.getRevisaoAtualSequencia())
                .orElseGet(() -> proposta.getRevisoes().isEmpty()
                        ? null
                        : proposta.getRevisoes().get(proposta.getRevisoes().size() - 1));
    }

    @Transactional(readOnly = true)
    public MotorFinanceiroComercialService.ResultadoAnaliseFinanceira simular(DadosRevisaoCommand command) {
        Carro carro = cadastroCarroService.buscarOuFalhar(command.getCarroId());
        PoliticaComercial politica = obterOuCriarPoliticaVigente(carro);
        List<CampanhaComercial> campanhas = resolverCampanhas(command.getCodigosCampanha());
        List<ItemFinanceiroRevisao> itensConsolidados = montarItensFinanceiros(
                carro, command.getItensExtras(), command.getAvaliacaoTroca());

        validarTrocaEPagamentos(
                command.getPrecoNegociado(),
                command.getAvaliacaoTroca(),
                command.getFinanciamento(),
                command.getPagamentos(),
                false);

        return motorFinanceiro.analisar(MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(carro.getCompra())
                .precoAnunciado(carro.obterPrecoAnunciadoEfetivo())
                .precoNegociado(command.getPrecoNegociado())
                .politica(politica)
                .itensFinanceiros(itensConsolidados)
                .campanhasAplicadas(campanhas)
                .build());
    }

    @Transactional
    public PropostaComercial criarProposta(DadosRevisaoCommand command) {
        Carro carro = cadastroCarroService.buscarOuFalhar(command.getCarroId());
        Cliente cliente = cadastroClienteService.buscarOuFalhar(command.getClienteId());

        validarDisponibilidadeVeiculoECliente(carro, cliente);

        PropostaComercial proposta = new PropostaComercial();
        proposta.setNumero(gerarNumeroProposta());
        proposta.setCarro(carro);
        proposta.setCliente(cliente);
        proposta.setVendedorIdentificador(
                command.getVendedorIdentificador() != null && !command.getVendedorIdentificador().isBlank()
                        ? command.getVendedorIdentificador().trim()
                        : "vendedor@concessionariagomez.com.br");
        proposta.setRevisaoAtualSequencia(1);
        proposta.setValidaAte(OffsetDateTime.now().plusDays(7));
        proposta.setStatus(StatusPropostaComercial.RASCUNHO);

        proposta = propostaRepository.save(proposta);

        RevisaoProposta revisao = construirNovaRevisao(proposta, 1, carro, command);
        proposta.getRevisoes().add(revisao);
        atualizarStatusPropostaConformeRevisao(proposta, revisao, command.isSubmeter());

        return propostaRepository.save(proposta);
    }

    @Transactional
    public PropostaComercial criarNovaRevisao(Long propostaId, DadosRevisaoCommand command) {
        PropostaComercial proposta = buscarOuFalhar(propostaId);
        if (proposta.getStatus() == StatusPropostaComercial.CONVERTIDA
                || proposta.getStatus() == StatusPropostaComercial.CANCELADA) {
            throw new ConflitoNegociacaoException(
                    "Não é possível gerar nova revisão para uma proposta já convertida ou cancelada.");
        }

        Carro carro = proposta.getCarro();
        Cliente cliente = proposta.getCliente();
        validarDisponibilidadeVeiculoECliente(carro, cliente);

        int novaSequencia = (proposta.getRevisaoAtualSequencia() != null ? proposta.getRevisaoAtualSequencia() : 0) + 1;
        proposta.setRevisaoAtualSequencia(novaSequencia);
        proposta.setValidaAte(OffsetDateTime.now().plusDays(7));

        RevisaoProposta novaRevisao = construirNovaRevisao(proposta, novaSequencia, carro, command);
        proposta.getRevisoes().add(novaRevisao);
        atualizarStatusPropostaConformeRevisao(proposta, novaRevisao, command.isSubmeter());

        return propostaRepository.save(proposta);
    }

    @Transactional
    public PropostaComercial decidirAprovacaoGerencial(
            Long propostaId,
            Integer sequenciaEsperada,
            String decisorAutenticado,
            PerfilOperadorComercial perfilDecisor,
            boolean aprovar,
            String motivo) {

        if (perfilDecisor == null || !perfilDecisor.podeAprovarExcecaoGerencial()) {
            throw new AcessoComercialNegadoException(
                    "Apenas operadores com perfil GERENTE ou ADMIN podem aprovar ou rejeitar propostas fora da autonomia.");
        }

        PropostaComercial proposta = buscarOuFalhar(propostaId);
        if (decisorAutenticado != null && decisorAutenticado.trim().equalsIgnoreCase(proposta.getVendedorIdentificador())) {
            throw new AcessoComercialNegadoException(
                    "Conflito de interesse: o vendedor responsável pela proposta não pode se autoaprovar como gerente.");
        }

        RevisaoProposta revisaoAtual = buscarRevisaoAtual(proposta);
        if (revisaoAtual == null) {
            throw new NegocioException("A proposta não possui revisão ativa para decisão.");
        }
        if (sequenciaEsperada != null && !sequenciaEsperada.equals(revisaoAtual.getSequencia())) {
            throw new ConflitoNegociacaoException(String.format(
                    "Conflito de revisão: você tentou decidir sobre a revisão %d, mas a revisão atual da proposta é a %d.",
                    sequenciaEsperada, revisaoAtual.getSequencia()));
        }

        if (revisaoAtual.getAlcadaAnalise() == AlcadaAnaliseFinanceira.BLOQUEADA_ABAIXO_PISO && aprovar) {
            throw new NegocioException(
                    "Proposta bloqueada abaixo do piso absoluto. O gerente não pode aprovar valores abaixo do piso absoluto sem uma política de exceção cadastrada.");
        }
        if (revisaoAtual.getAlcadaAnalise() == AlcadaAnaliseFinanceira.PENDENTE_DADOS_OBRIGATORIOS && aprovar) {
            throw new NegocioException(
                    "Não é possível aprovar uma proposta enquanto existirem custos ou pendências obrigatórias sem confirmação.");
        }

        revisaoAtual.setSubmetida(Boolean.TRUE);

        DecisaoAprovacao decisao = new DecisaoAprovacao();
        decisao.setRevisao(revisaoAtual);
        decisao.setDecisorAutenticado(decisorAutenticado != null && !decisorAutenticado.isBlank()
                ? decisorAutenticado.trim()
                : "gerente@concessionariagomez.com.br");
        decisao.setAlcada(revisaoAtual.getAlcadaAnalise());
        decisao.setResultado(aprovar
                ? ResultadoDecisaoAprovacao.APROVADA_GERENCIA
                : ResultadoDecisaoAprovacao.REJEITADA_GERENCIA);
        decisao.setMotivo(motivo);
        revisaoAtual.getDecisoes().add(decisao);

        proposta.setStatus(aprovar ? StatusPropostaComercial.APROVADA : StatusPropostaComercial.REJEITADA);
        return propostaRepository.save(proposta);
    }

    @Transactional
    public PropostaComercial reservarVeiculoTemporariamente(Long propostaId, String responsavel, int horasValidade) {
        PropostaComercial proposta = buscarOuFalhar(propostaId);
        Carro carro = proposta.getCarro();

        if (!Boolean.TRUE.equals(carro.getAtivo())
                || carro.getDisponibilidadeComercial() == DisponibilidadeComercial.VENDIDO) {
            throw new ConflitoNegociacaoException("O veículo já foi vendido ou está inativo e não pode ser reservado.");
        }
        if (carro.getDisponibilidadeComercial() == DisponibilidadeComercial.RESERVADO
                && carro.getReservaValidaAte() != null
                && carro.getReservaValidaAte().isAfter(OffsetDateTime.now())
                && carro.getReservaResponsavel() != null
                && !carro.getReservaResponsavel().equalsIgnoreCase(proposta.getNumero())) {
            throw new ConflitoNegociacaoException(
                    "O veículo já possui reserva ativa vinculada a outra proposta (" + carro.getReservaResponsavel() + ").");
        }

        carro.setDisponibilidadeComercial(DisponibilidadeComercial.RESERVADO);
        carro.setReservaResponsavel(proposta.getNumero());
        carro.setReservaValidaAte(OffsetDateTime.now().plusHours(Math.max(1, horasValidade)));
        cadastroCarroService.salvar(carro);

        return proposta;
    }

    @Transactional
    public Venda converterEmVenda(Long propostaId, Integer sequenciaRevisaoAutorizada, String observacoesFechamento) {
        PropostaComercial proposta = buscarOuFalhar(propostaId);
        RevisaoProposta revisaoAtual = buscarRevisaoAtual(proposta);

        if (revisaoAtual == null) {
            throw new NegocioException("Proposta sem revisão para conversão.");
        }
        if (sequenciaRevisaoAutorizada != null && !sequenciaRevisaoAutorizada.equals(revisaoAtual.getSequencia())) {
            throw new ConflitoNegociacaoException(String.format(
                    "A revisão informada (%d) está obsoleta. A revisão atual da proposta é a %d.",
                    sequenciaRevisaoAutorizada, revisaoAtual.getSequencia()));
        }

        // Idempotência: se esta revisão já foi convertida em venda, retorna a venda existente sem duplicar!
        var vendaExistente = vendaRepository.findByRevisaoConvertidaId(revisaoAtual.getId());
        if (vendaExistente.isPresent()) {
            return vendaExistente.get();
        }

        if (proposta.getStatus() != StatusPropostaComercial.APROVADA) {
            throw new NegocioException(
                    "Apenas propostas com revisão APROVADA podem ser convertidas em venda. Status atual: " + proposta.getStatus());
        }
        if (proposta.getValidaAte() != null && proposta.getValidaAte().isBefore(OffsetDateTime.now())) {
            proposta.setStatus(StatusPropostaComercial.EXPIRADA);
            propostaRepository.save(proposta);
            throw new ConflitoNegociacaoException("A proposta comercial expirou e exige nova revisão e aprovação.");
        }

        Carro carro = proposta.getCarro();
        Cliente cliente = proposta.getCliente();

        // Verifica se o custo de aquisição do veículo mudou após a aprovação da revisão
        if (carro.getCompra().compareTo(revisaoAtual.getCustoAquisicaoSnapshot()) != 0) {
            throw new ConflitoNegociacaoException(
                    "O custo de aquisição do veículo foi alterado após a revisão. Gere uma nova revisão para revalidar a margem.");
        }

        if (!Boolean.TRUE.equals(carro.getAtivo())
                || carro.getDisponibilidadeComercial() == DisponibilidadeComercial.VENDIDO) {
            throw new ConflitoNegociacaoException(
                    String.format("O veículo '%s %s' não está mais disponível no estoque.", carro.getMarca(), carro.getModelo()));
        }

        if (carro.getDisponibilidadeComercial() == DisponibilidadeComercial.RESERVADO
                && carro.getReservaValidaAte() != null
                && carro.getReservaValidaAte().isAfter(OffsetDateTime.now())
                && carro.getReservaResponsavel() != null
                && !carro.getReservaResponsavel().equalsIgnoreCase(proposta.getNumero())) {
            throw new ConflitoNegociacaoException(
                    "O veículo está reservado para outra proposta ativa (" + carro.getReservaResponsavel() + ").");
        }

        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new NegocioException("O cliente está inativo e não pode concluir a compra.");
        }

        validarTrocaEPagamentos(
                revisaoAtual.getPrecoNegociado(),
                revisaoAtual.getAvaliacaoTroca(),
                revisaoAtual.getFinanciamento(),
                revisaoAtual.getPagamentos(),
                true);

        // Congela o fechamento, marca o veículo vendido e incorpora eventual veículo usado ao estoque
        carro.marcarComoVendido();
        cadastroCarroService.salvar(carro);

        if (revisaoAtual.getAvaliacaoTroca() != null) {
            incorporarUsadoAoEstoqueNoFechamento(revisaoAtual.getAvaliacaoTroca());
        }

        proposta.setStatus(StatusPropostaComercial.CONVERTIDA);
        propostaRepository.save(proposta);

        Venda venda = new Venda();
        venda.setCarro(carro);
        venda.setCliente(cliente);
        venda.setRevisaoConvertida(revisaoAtual);
        venda.setOrigemLegada(Boolean.FALSE);
        venda.setCancelada(Boolean.FALSE);
        venda.setValorVenda(revisaoAtual.getPrecoNegociado());
        venda.setObservacoes(observacoesFechamento != null && !observacoesFechamento.isBlank()
                ? observacoesFechamento
                : "Convertida da proposta " + proposta.getNumero() + " (Rev. " + revisaoAtual.getSequencia() + ")");

        return vendaRepository.save(venda);
    }

    private RevisaoProposta construirNovaRevisao(
            PropostaComercial proposta,
            int sequencia,
            Carro carro,
            DadosRevisaoCommand command) {

        PoliticaComercial politica = obterOuCriarPoliticaVigente(carro);
        List<CampanhaComercial> campanhas = resolverCampanhas(command.getCodigosCampanha());
        List<ItemFinanceiroRevisao> itensConsolidados = montarItensFinanceiros(
                carro, command.getItensExtras(), command.getAvaliacaoTroca());

        validarTrocaEPagamentos(
                command.getPrecoNegociado(),
                command.getAvaliacaoTroca(),
                command.getFinanciamento(),
                command.getPagamentos(),
                command.isSubmeter());

        var analise = motorFinanceiro.analisar(MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(carro.getCompra())
                .precoAnunciado(carro.obterPrecoAnunciadoEfetivo())
                .precoNegociado(command.getPrecoNegociado())
                .politica(politica)
                .itensFinanceiros(itensConsolidados)
                .campanhasAplicadas(campanhas)
                .build());

        RevisaoProposta revisao = new RevisaoProposta();
        revisao.setProposta(proposta);
        revisao.setSequencia(sequencia);
        revisao.setPoliticaComercial(politica);
        revisao.setCustoAquisicaoSnapshot(analise.getCustoAquisicao());
        revisao.setPrecoAnunciadoSnapshot(analise.getPrecoAnunciado());
        revisao.setPrecoSugeridoSnapshot(carro.calcularPrecoVendaSugerido());
        revisao.setPrecoNegociado(analise.getPrecoNegociado());
        revisao.setCustosFixosOperacao(analise.getCustosFixosOperacao());
        revisao.setSomaTaxasProporcionais(analise.getSomaTaxasProporcionais());
        revisao.setReceitasAdicionaisElegiveis(analise.getReceitasAdicionaisElegiveis());
        revisao.setContribuicaoEstimada(analise.getContribuicaoEstimada());
        revisao.setMargemContribuicaoEstimada(analise.getMargemContribuicaoEstimada());
        revisao.setDescontoReais(analise.getDescontoReais());
        revisao.setDescontoPercentual(analise.getDescontoPercentual());
        revisao.setMarkupAquisicao(analise.getMarkupAquisicao());
        revisao.setPisoAutonomiaCalculado(analise.getPisoAutonomia());
        revisao.setPisoAbsolutoCalculado(analise.getPisoAbsoluto());
        revisao.setAlcadaAnalise(analise.getAlcada());
        revisao.setPossuiCustosPendentes(analise.isPossuiCustosPendentes());
        revisao.setJustificativaVendedor(command.getJustificativaVendedor());
        revisao.setSubmetida(command.isSubmeter());

        for (ItemFinanceiroRevisao item : itensConsolidados) {
            item.setRevisao(revisao);
            revisao.getItensFinanceiros().add(item);
        }

        if (command.getPagamentos() != null) {
            for (PagamentoProposta pgto : command.getPagamentos()) {
                pgto.setRevisao(revisao);
                revisao.getPagamentos().add(pgto);
            }
        }

        if (command.getFinanciamento() != null) {
            FinanciamentoProposta fin = command.getFinanciamento();
            fin.setRevisao(revisao);
            revisao.setFinanciamento(fin);
        }

        if (command.getAvaliacaoTroca() != null) {
            AvaliacaoTroca troca = command.getAvaliacaoTroca();
            troca.setCreditoLiquido(troca.calcularCreditoLiquido());
            troca.setRevisao(revisao);
            revisao.setAvaliacaoTroca(troca);
        }

        return revisao;
    }

    private void atualizarStatusPropostaConformeRevisao(
            PropostaComercial proposta,
            RevisaoProposta revisao,
            boolean submeter) {

        if (!submeter) {
            proposta.setStatus(StatusPropostaComercial.EM_NEGOCIACAO);
            return;
        }

        if (revisao.getAlcadaAnalise() == AlcadaAnaliseFinanceira.DENTRO_AUTONOMIA) {
            DecisaoAprovacao auto = new DecisaoAprovacao();
            auto.setRevisao(revisao);
            auto.setDecisorAutenticado("POLITICA_COMERCIAL_AUTOMATICA");
            auto.setAlcada(AlcadaAnaliseFinanceira.DENTRO_AUTONOMIA);
            auto.setResultado(ResultadoDecisaoAprovacao.APROVADA_POLITICA_AUTOMATICA);
            auto.setMotivo("Liberada automaticamente por cumprir margem e limite de desconto da política v"
                    + revisao.getPoliticaComercial().getVersao());
            revisao.getDecisoes().add(auto);
            proposta.setStatus(StatusPropostaComercial.APROVADA);
        } else if (revisao.getAlcadaAnalise() == AlcadaAnaliseFinanceira.EXIGE_APROVACAO_GERENCIAL) {
            proposta.setStatus(StatusPropostaComercial.PENDENTE_APROVACAO);
        } else {
            throw new NegocioException(
                    "A revisão não pode ser submetida para aprovação pois está bloqueada abaixo do piso absoluto ou possui pendências obrigatórias.");
        }
    }

    private PoliticaComercial obterOuCriarPoliticaVigente(Carro carro) {
        OffsetDateTime agora = OffsetDateTime.now();
        List<PoliticaComercial> candidatas = new ArrayList<>();
        if (carro != null && carro.getId() != null) {
            candidatas.addAll(politicaRepository.findVigentes(
                    ContextoPoliticaComercial.EXCECAO_VEICULO, carro.getId(), agora));
        }
        candidatas.addAll(politicaRepository.findVigentes(
                ContextoPoliticaComercial.LOJA_PADRAO, null, agora));

        if (candidatas.isEmpty()) {
            PoliticaComercial padrao = new PoliticaComercial();
            padrao.setContexto(ContextoPoliticaComercial.LOJA_PADRAO);
            padrao.setVersao(1);
            padrao.setMargemDesejada(new BigDecimal("0.100000"));
            padrao.setMargemAutonomia(new BigDecimal("0.070000"));
            padrao.setMargemPisoAbsoluto(new BigDecimal("0.050000"));
            padrao.setDescontoMaximoAutonomiaReais(new BigDecimal("5000.00"));
            padrao.setDescontoMaximoAutonomiaPercentual(new BigDecimal("0.050000"));
            padrao.setResponsavelCadastro("sistema@concessionariagomez.com.br");
            padrao.setVigenteAPartirDe(agora.minusYears(5));
            padrao.setAtiva(true);
            candidatas.add(politicaRepository.save(padrao));
        }

        return motorFinanceiro.resolverPoliticaVigente(carro, candidatas, agora);
    }

    private List<ItemFinanceiroRevisao> montarItensFinanceiros(
            Carro carro,
            List<ItemFinanceiroRevisao> itensExtras,
            AvaliacaoTroca avaliacaoTroca) {

        List<ItemFinanceiroRevisao> lista = new ArrayList<>();
        if (carro != null && carro.getId() != null) {
            List<CustoVeiculo> custosCadastrados = custoVeiculoRepository.findByCarroIdOrderByIdAsc(carro.getId());
            for (CustoVeiculo cv : custosCadastrados) {
                ItemFinanceiroRevisao copia = new ItemFinanceiroRevisao();
                copia.setNatureza(cv.getNatureza());
                copia.setBaseCalculo(cv.getBaseCalculo());
                copia.setValor(cv.getValor());
                copia.setConfirmado(cv.getConfirmado());
                copia.setDescricao(cv.getDescricao());
                copia.setOrigem("CUSTO_VEICULO");
                lista.add(copia);
            }
        }

        if (itensExtras != null) {
            for (ItemFinanceiroRevisao extra : itensExtras) {
                ItemFinanceiroRevisao copia = new ItemFinanceiroRevisao();
                copia.setNatureza(extra.getNatureza());
                copia.setBaseCalculo(extra.getBaseCalculo());
                copia.setValor(extra.getValor());
                copia.setConfirmado(extra.getConfirmado());
                copia.setDescricao(extra.getDescricao());
                copia.setOrigem(extra.getOrigem() != null ? extra.getOrigem() : "PROPOSTA");
                lista.add(copia);
            }
        }

        // Etapa 7.5: Superavaliação do usado suportada pela loja entra como concessão econômica (custo fixo na operação)
        if (avaliacaoTroca != null) {
            BigDecimal superavaliacao = avaliacaoTroca.calcularSuperavaliacaoSuportadaPelaLoja();
            if (superavaliacao.compareTo(BigDecimal.ZERO) > 0) {
                ItemFinanceiroRevisao itemSuper = new ItemFinanceiroRevisao();
                itemSuper.setNatureza(NaturezaItemFinanceiro.SUPERAVALIACAO_TROCA);
                itemSuper.setBaseCalculo(BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS);
                itemSuper.setValor(superavaliacao);
                itemSuper.setConfirmado(!Boolean.TRUE.equals(avaliacaoTroca.getPossuiPendenciaDocumental()));
                itemSuper.setDescricao("Superavaliação suportada pela loja no usado: " + avaliacaoTroca.getIdentificacaoVeiculoUsado());
                itemSuper.setOrigem("AVALIACAO_TROCA");
                lista.add(itemSuper);
            }
        }

        return lista;
    }

    private List<CampanhaComercial> resolverCampanhas(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return Collections.emptyList();
        }
        List<CampanhaComercial> encontradas = new ArrayList<>();
        for (String codigo : codigos) {
            if (codigo == null || codigo.isBlank()) continue;
            CampanhaComercial camp = campanhaRepository.findByCodigo(codigo.trim())
                    .orElseThrow(() -> new NegocioException("Campanha comercial não encontrada: " + codigo));
            encontradas.add(camp);
        }
        return encontradas;
    }

    private void validarTrocaEPagamentos(
            BigDecimal precoNegociado,
            AvaliacaoTroca troca,
            FinanciamentoProposta financiamento,
            List<PagamentoProposta> pagamentos,
            boolean exigirConciliacaoCompleta) {

        if (troca != null) {
            if (troca.possuiSaldoDevedorSuperiorAvaliacao()) {
                throw new NegocioException(
                        "O veículo dado na troca possui saldo de quitação superior ao valor comercial aceito (crédito líquido negativo).");
            }
            if (exigirConciliacaoCompleta && Boolean.TRUE.equals(troca.getPossuiPendenciaDocumental())) {
                throw new NegocioException(
                        "O veículo dado na troca possui pendência documental não resolvida: " + troca.getDescricaoPendencias());
            }
        }

        if (pagamentos != null && !pagamentos.isEmpty()) {
            BigDecimal somaPagamentos = pagamentos.stream()
                    .map(PagamentoProposta::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            if (troca != null) {
                BigDecimal creditoTrocaNosPagamentos = pagamentos.stream()
                        .filter(p -> p.getModalidade() == ModalidadePagamentoProposta.CREDITO_LIQUIDO_TROCA)
                        .map(PagamentoProposta::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

                if (creditoTrocaNosPagamentos.compareTo(BigDecimal.ZERO) > 0
                        && creditoTrocaNosPagamentos.compareTo(troca.calcularCreditoLiquido()) != 0) {
                    throw new NegocioException(String.format(
                            "Inconsistência na troca: o pagamento de crédito de troca (R$ %s) diverge do crédito líquido apurado após quitação (R$ %s).",
                            creditoTrocaNosPagamentos, troca.calcularCreditoLiquido()));
                }
            }

            if (financiamento != null) {
                BigDecimal finNosPagamentos = pagamentos.stream()
                        .filter(p -> p.getModalidade() == ModalidadePagamentoProposta.FINANCIAMENTO)
                        .map(PagamentoProposta::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

                if (finNosPagamentos.compareTo(BigDecimal.ZERO) > 0
                        && finNosPagamentos.compareTo(financiamento.getPrincipalFinanciado().setScale(2, RoundingMode.HALF_UP)) != 0) {
                    throw new NegocioException(
                            "O valor na modalidade FINANCIAMENTO diverge do principal financiado informado na proposta.");
                }
            }

            if (exigirConciliacaoCompleta
                    && somaPagamentos.compareTo(precoNegociado.setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new NegocioException(String.format(
                        "A composição de pagamentos (R$ %s) não concilia exatamente com o preço negociado (R$ %s).",
                        somaPagamentos, precoNegociado.setScale(2, RoundingMode.HALF_UP)));
            }
        }
    }

    private void incorporarUsadoAoEstoqueNoFechamento(AvaliacaoTroca troca) {
        Carro usado = new Carro();
        String idUsado = troca.getIdentificacaoVeiculoUsado() != null
                ? troca.getIdentificacaoVeiculoUsado().trim()
                : "Usado Troca";
        String[] partes = idUsado.split("\\s+", 2);
        usado.setMarca(partes[0]);
        usado.setModelo(partes.length > 1 ? partes[1] : idUsado);
        usado.setAno(OffsetDateTime.now().getYear() - 3);
        usado.setCompra(troca.getValorAvaliacaoTecnica());
        usado.setQuilometragem(50000);
        usado.setAtivo(Boolean.TRUE);
        usado.setDisponibilidadeComercial(DisponibilidadeComercial.DISPONIVEL);
        cadastroCarroService.salvar(usado);
    }

    private void validarDisponibilidadeVeiculoECliente(Carro carro, Cliente cliente) {
        if (!Boolean.TRUE.equals(carro.getAtivo())
                || carro.getDisponibilidadeComercial() == DisponibilidadeComercial.VENDIDO) {
            throw new NegocioException(
                    String.format("O veículo '%s %s' (código %d) não está disponível para negociação.",
                            carro.getMarca(), carro.getModelo(), carro.getId()));
        }
        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new NegocioException(
                    String.format("O cliente '%s' (código %d) está inativo.",
                            cliente.getNome(), cliente.getId()));
        }
    }

    private String gerarNumeroProposta() {
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PROP-" + OffsetDateTime.now().getYear() + "-" + sufixo;
    }
}
