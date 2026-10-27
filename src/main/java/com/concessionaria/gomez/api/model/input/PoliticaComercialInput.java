package com.concessionaria.gomez.api.model.input;

import com.concessionaria.gomez.domain.model.ContextoPoliticaComercial;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PoliticaComercialInput {

    @NotNull
    private ContextoPoliticaComercial contexto = ContextoPoliticaComercial.LOJA_PADRAO;

    private Long carroId;

    @NotNull
    @PositiveOrZero
    private BigDecimal margemDesejada;

    @NotNull
    @PositiveOrZero
    private BigDecimal margemAutonomia;

    @NotNull
    @PositiveOrZero
    private BigDecimal margemPisoAbsoluto;

    @PositiveOrZero
    private BigDecimal descontoMaximoAutonomiaReais;

    @PositiveOrZero
    private BigDecimal descontoMaximoAutonomiaPercentual;
}
