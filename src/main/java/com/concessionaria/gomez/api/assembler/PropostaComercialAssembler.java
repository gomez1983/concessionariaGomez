package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.controller.CarroController;
import com.concessionaria.gomez.api.controller.ClienteController;
import com.concessionaria.gomez.api.controller.PropostaComercialController;
import com.concessionaria.gomez.api.model.CarroResumoModel;
import com.concessionaria.gomez.api.model.ClienteResumoModel;
import com.concessionaria.gomez.api.model.PropostaComercialModel;
import com.concessionaria.gomez.api.model.SimulacaoPropostaModel;
import com.concessionaria.gomez.api.model.input.PropostaComercialInput;
import com.concessionaria.gomez.domain.model.*;
import com.concessionaria.gomez.domain.service.GestaoPropostaComercialService;
import com.concessionaria.gomez.domain.service.MotorFinanceiroComercialService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PropostaComercialAssembler {

    public GestaoPropostaComercialService.DadosRevisaoCommand toCommand(
            PropostaComercialInput input,
            String vendedorIdentificador) {

        List<ItemFinanceiroRevisao> itens = input.getItensExtras() == null
                ? Collections.emptyList()
                : input.getItensExtras().stream().map(i -> {
                    ItemFinanceiroRevisao item = new ItemFinanceiroRevisao();
                    item.setNatureza(i.getNatureza());
                    item.setBaseCalculo(i.getBaseCalculo());
                    item.setValor(i.getValor());
                    item.setConfirmado(i.isConfirmado());
                    item.setDescricao(i.getDescricao());
                    item.setOrigem("PROPOSTA");
                    return item;
                }).collect(Collectors.toList());

        List<PagamentoProposta> pagamentos = input.getPagamentos() == null
                ? Collections.emptyList()
                : input.getPagamentos().stream().map(p -> {
                    PagamentoProposta pg = new PagamentoProposta();
                    pg.setModalidade(p.getModalidade());
                    pg.setValor(p.getValor());
                    pg.setObservacao(p.getObservacao());
                    return pg;
                }).collect(Collectors.toList());

        FinanciamentoProposta financiamento = null;
        if (input.getFinanciamento() != null) {
            var f = input.getFinanciamento();
            financiamento = new FinanciamentoProposta();
            financiamento.setInstituicaoFinanceira(f.getInstituicaoFinanceira());
            financiamento.setPrincipalFinanciado(f.getPrincipalFinanciado());
            financiamento.setEntradaMonetariaRelacionada(
                    f.getEntradaMonetariaRelacionada() != null ? f.getEntradaMonetariaRelacionada() : BigDecimal.ZERO);
            financiamento.setPrazoMeses(f.getPrazoMeses());
            financiamento.setTaxaMensal(f.getTaxaMensal());
            financiamento.setCetAnualInformado(f.getCetAnualInformado());
            financiamento.setValorParcela(f.getValorParcela());
            financiamento.setApenasEstimativa(f.isApenasEstimativa());
            financiamento.setSituacaoExterna(f.getSituacaoExterna() != null ? f.getSituacaoExterna() : "SIMULADO");
        }

        AvaliacaoTroca troca = null;
        if (input.getAvaliacaoTroca() != null) {
            var t = input.getAvaliacaoTroca();
            troca = new AvaliacaoTroca();
            troca.setIdentificacaoVeiculoUsado(t.getIdentificacaoVeiculoUsado());
            troca.setValorAvaliacaoTecnica(t.getValorAvaliacaoTecnica());
            troca.setValorComercialAceito(t.getValorComercialAceito());
            troca.setSaldoQuitacaoVerificado(
                    t.getSaldoQuitacaoVerificado() != null ? t.getSaldoQuitacaoVerificado() : BigDecimal.ZERO);
            troca.setPossuiPendenciaDocumental(t.isPossuiPendenciaDocumental());
            troca.setDescricaoPendencias(t.getDescricaoPendencias());
            troca.setCreditoLiquido(troca.calcularCreditoLiquido());
        }

        return GestaoPropostaComercialService.DadosRevisaoCommand.builder()
                .carroId(input.getCarroId())
                .clienteId(input.getClienteId())
                .vendedorIdentificador(vendedorIdentificador)
                .precoNegociado(input.getPrecoNegociado())
                .justificativaVendedor(input.getJustificativaVendedor())
                .submeter(input.isSubmeter())
                .itensExtras(itens)
                .pagamentos(pagamentos)
                .financiamento(financiamento)
                .avaliacaoTroca(troca)
                .codigosCampanha(input.getCodigosCampanha() != null ? input.getCodigosCampanha() : Collections.emptyList())
                .build();
    }

    public SimulacaoPropostaModel toSimulacaoModel(
            MotorFinanceiroComercialService.ResultadoAnaliseFinanceira analise,
            PerfilOperadorComercial perfil) {

        boolean podeVerInterno = perfil != null && perfil.podeVerCustosEMargensInternas();

        var builder = SimulacaoPropostaModel.builder()
                .precoAnunciado(analise.getPrecoAnunciado())
                .precoNegociado(analise.getPrecoNegociado())
                .descontoReais(analise.getDescontoReais())
                .descontoPercentual(analise.getDescontoPercentual())
                .alcada(analise.getAlcada())
                .possuiCustosPendentes(analise.isPossuiCustosPendentes())
                .motivosAlcada(analise.getMotivosAlcada())
                .pisoAutonomia(analise.getPisoAutonomia());

        if (podeVerInterno) {
            builder.custoAquisicao(analise.getCustoAquisicao())
                    .custosFixosOperacao(analise.getCustosFixosOperacao())
                    .somaTaxasProporcionais(analise.getSomaTaxasProporcionais())
                    .receitasAdicionaisElegiveis(analise.getReceitasAdicionaisElegiveis())
                    .contribuicaoEstimada(analise.getContribuicaoEstimada())
                    .margemContribuicaoEstimada(analise.getMargemContribuicaoEstimada())
                    .markupAquisicao(analise.getMarkupAquisicao())
                    .pisoDesejado(analise.getPisoDesejado())
                    .pisoAbsoluto(analise.getPisoAbsoluto());
        }

        return builder.build();
    }

    public PropostaComercialModel toModel(
            PropostaComercial proposta,
            RevisaoProposta revisaoAtual,
            PerfilOperadorComercial perfil) {

        boolean podeVerInterno = perfil != null && perfil.podeVerCustosEMargensInternas();

        CarroResumoModel carroResumo = new CarroResumoModel();
        if (proposta.getCarro() != null) {
            carroResumo.setId(proposta.getCarro().getId());
            carroResumo.setMarca(proposta.getCarro().getMarca());
            carroResumo.setModelo(proposta.getCarro().getModelo());
            carroResumo.setAno(proposta.getCarro().getAno());
        }

        ClienteResumoModel clienteResumo = new ClienteResumoModel();
        if (proposta.getCliente() != null) {
            clienteResumo.setId(proposta.getCliente().getId());
            clienteResumo.setNome(proposta.getCliente().getNome());
            clienteResumo.setCpfCnpj(proposta.getCliente().getCpfCnpj());
        }

        PropostaComercialModel.RevisaoPropostaModel revisaoModel = null;
        PropostaComercialModel.ResumoClienteModel resumoCliente = null;

        if (revisaoAtual != null) {
            var revBuilder = PropostaComercialModel.RevisaoPropostaModel.builder()
                    .id(revisaoAtual.getId())
                    .sequencia(revisaoAtual.getSequencia())
                    .precoAnunciado(revisaoAtual.getPrecoAnunciadoSnapshot())
                    .precoSugerido(revisaoAtual.getPrecoSugeridoSnapshot())
                    .precoNegociado(revisaoAtual.getPrecoNegociado())
                    .descontoReais(revisaoAtual.getDescontoReais())
                    .descontoPercentual(revisaoAtual.getDescontoPercentual())
                    .alcadaAnalise(revisaoAtual.getAlcadaAnalise())
                    .possuiCustosPendentes(revisaoAtual.getPossuiCustosPendentes())
                    .submetida(revisaoAtual.getSubmetida())
                    .justificativaVendedor(revisaoAtual.getJustificativaVendedor())
                    .dataCriacao(revisaoAtual.getDataCriacao())
                    .pisoAutonomia(revisaoAtual.getPisoAutonomiaCalculado());

            if (podeVerInterno) {
                revBuilder.custoAquisicao(revisaoAtual.getCustoAquisicaoSnapshot())
                        .custosFixosOperacao(revisaoAtual.getCustosFixosOperacao())
                        .somaTaxasProporcionais(revisaoAtual.getSomaTaxasProporcionais())
                        .receitasAdicionaisElegiveis(revisaoAtual.getReceitasAdicionaisElegiveis())
                        .contribuicaoEstimada(revisaoAtual.getContribuicaoEstimada())
                        .margemContribuicaoEstimada(revisaoAtual.getMargemContribuicaoEstimada())
                        .markupAquisicao(revisaoAtual.getMarkupAquisicao())
                        .pisoAbsoluto(revisaoAtual.getPisoAbsolutoCalculado());
            }

            if (revisaoAtual.getAvaliacaoTroca() != null) {
                var t = revisaoAtual.getAvaliacaoTroca();
                revBuilder.avaliacaoTroca(PropostaComercialModel.AvaliacaoTrocaModel.builder()
                        .identificacaoVeiculoUsado(t.getIdentificacaoVeiculoUsado())
                        .valorAvaliacaoTecnica(podeVerInterno ? t.getValorAvaliacaoTecnica() : null)
                        .valorComercialAceito(t.getValorComercialAceito())
                        .saldoQuitacaoVerificado(t.getSaldoQuitacaoVerificado())
                        .creditoLiquido(t.getCreditoLiquido())
                        .possuiPendenciaDocumental(t.getPossuiPendenciaDocumental())
                        .descricaoPendencias(t.getDescricaoPendencias())
                        .build());
            }

            if (revisaoAtual.getFinanciamento() != null) {
                var f = revisaoAtual.getFinanciamento();
                revBuilder.financiamento(PropostaComercialModel.FinanciamentoModel.builder()
                        .instituicaoFinanceira(f.getInstituicaoFinanceira())
                        .principalFinanciado(f.getPrincipalFinanciado())
                        .entradaMonetariaRelacionada(f.getEntradaMonetariaRelacionada())
                        .prazoMeses(f.getPrazoMeses())
                        .taxaMensal(f.getTaxaMensal())
                        .cetAnualInformado(f.getCetAnualInformado())
                        .valorParcela(f.getValorParcela())
                        .apenasEstimativa(f.getApenasEstimativa())
                        .situacaoExterna(f.getSituacaoExterna())
                        .build());
            }

            if (revisaoAtual.getPagamentos() != null) {
                revBuilder.pagamentos(revisaoAtual.getPagamentos().stream()
                        .map(p -> PropostaComercialModel.PagamentoModel.builder()
                                .modalidade(p.getModalidade())
                                .valor(p.getValor())
                                .observacao(p.getObservacao())
                                .build())
                        .collect(Collectors.toList()));
            }

            if (revisaoAtual.getDecisoes() != null) {
                revBuilder.decisoes(revisaoAtual.getDecisoes().stream()
                        .map(d -> PropostaComercialModel.DecisaoModel.builder()
                                .decisorAutenticado(d.getDecisorAutenticado())
                                .resultado(d.getResultado())
                                .alcada(d.getAlcada())
                                .motivo(d.getMotivo())
                                .dataDecisao(d.getDataDecisao())
                                .build())
                        .collect(Collectors.toList()));
            }

            revisaoModel = revBuilder.build();

            resumoCliente = PropostaComercialModel.ResumoClienteModel.builder()
                    .numeroProposta(proposta.getNumero())
                    .veiculo(carroResumo.getMarca() + " " + carroResumo.getModelo() + " (" + carroResumo.getAno() + ")")
                    .cliente(clienteResumo.getNome())
                    .precoAnunciado(revisaoAtual.getPrecoAnunciadoSnapshot())
                    .descontoConcedido(revisaoAtual.getDescontoReais())
                    .precoFinalProposto(revisaoAtual.getPrecoNegociado())
                    .creditoLiquidoUsadoNaTroca(revisaoAtual.getAvaliacaoTroca() != null
                            ? revisaoAtual.getAvaliacaoTroca().getCreditoLiquido()
                            : null)
                    .valorFinanciado(revisaoAtual.getFinanciamento() != null
                            ? revisaoAtual.getFinanciamento().getPrincipalFinanciado()
                            : null)
                    .prazoFinanciamentoMeses(revisaoAtual.getFinanciamento() != null
                            ? revisaoAtual.getFinanciamento().getPrazoMeses()
                            : null)
                    .validaAte(proposta.getValidaAte())
                    .build();
        }

        PropostaComercialModel model = PropostaComercialModel.builder()
                .id(proposta.getId())
                .numero(proposta.getNumero())
                .carro(carroResumo)
                .cliente(clienteResumo)
                .vendedorIdentificador(proposta.getVendedorIdentificador())
                .status(proposta.getStatus())
                .revisaoAtualSequencia(proposta.getRevisaoAtualSequencia())
                .validaAte(proposta.getValidaAte())
                .dataCriacao(proposta.getDataCriacao())
                .revisaoAtual(revisaoModel)
                .resumoParaCliente(resumoCliente)
                .build();

        if (proposta.getId() != null) {
            model.add(linkTo(methodOn(PropostaComercialController.class).buscar(proposta.getId(), null)).withSelfRel());
            model.add(linkTo(PropostaComercialController.class).withRel("propostas"));

            if (proposta.getCarro() != null && proposta.getCarro().getId() != null) {
                model.add(linkTo(methodOn(CarroController.class).buscar(proposta.getCarro().getId())).withRel("carro"));
            }
            if (proposta.getCliente() != null && proposta.getCliente().getId() != null) {
                model.add(linkTo(methodOn(ClienteController.class).buscar(proposta.getCliente().getId())).withRel("cliente"));
            }

            if (proposta.getStatus() != StatusPropostaComercial.CONVERTIDA
                    && proposta.getStatus() != StatusPropostaComercial.CANCELADA) {
                model.add(linkTo(PropostaComercialController.class)
                        .slash(proposta.getId()).slash("revisoes").withRel("nova-revisao"));
            }
            if (proposta.getStatus() == StatusPropostaComercial.PENDENTE_APROVACAO) {
                model.add(linkTo(PropostaComercialController.class)
                        .slash(proposta.getId()).slash("aprovacoes").withRel("decidir-aprovacao"));
            }
            if (proposta.getStatus() == StatusPropostaComercial.APROVADA) {
                model.add(linkTo(PropostaComercialController.class)
                        .slash(proposta.getId()).slash("converter-venda").withRel("converter-venda"));
            }
        }

        return model;
    }
}
