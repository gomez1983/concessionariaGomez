package com.concessionaria.gomez.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardModel(
        long totalVeiculos,
        long ativos,
        long zeroKm,
        BigDecimal valorInvestido,
        BigDecimal vendaSugerida,
        BigDecimal margemPrevista,
        Map<String, Long> marcas,
        long propostasEmNegociacao,
        long propostasPendentesAprovacao,
        long propostasAprovadas,
        long propostasConvertidas,
        BigDecimal faturamentoRealizado,
        BigDecimal contribuicaoRealizada) { }
