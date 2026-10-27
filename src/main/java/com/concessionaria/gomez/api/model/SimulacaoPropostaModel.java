package com.concessionaria.gomez.api.model;

import com.concessionaria.gomez.domain.model.AlcadaAnaliseFinanceira;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulacaoPropostaModel {

    private BigDecimal precoAnunciado;
    private BigDecimal precoNegociado;
    private BigDecimal descontoReais;
    private BigDecimal descontoPercentual;
    private AlcadaAnaliseFinanceira alcada;
    private boolean possuiCustosPendentes;
    private List<String> motivosAlcada;

    // Exibido para orientar o vendedor sobre até onde vai sua autonomia sem expor o custo de aquisição
    private BigDecimal pisoAutonomia;

    // Campos restritos a GERENTE / ADMIN
    private BigDecimal custoAquisicao;
    private BigDecimal custosFixosOperacao;
    private BigDecimal somaTaxasProporcionais;
    private BigDecimal receitasAdicionaisElegiveis;
    private BigDecimal contribuicaoEstimada;
    private BigDecimal margemContribuicaoEstimada;
    private BigDecimal markupAquisicao;
    private BigDecimal pisoDesejado;
    private BigDecimal pisoAbsoluto;
}
