package com.concessionaria.gomez.domain.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CarroFilter {

    @Schema(description = "Marca do veículo para busca parcial", example = "Mercedes")
    private String marca;

    @Schema(description = "Modelo do veículo para busca parcial", example = "Classe A")
    private String modelo;

    @Schema(description = "Ano de fabricação", example = "2022")
    private Integer ano;

    @Schema(description = "Preço de compra mínimo", example = "50000.00")
    private BigDecimal minValor;

    @Schema(description = "Preço de compra máximo", example = "200000.00")
    private BigDecimal maxValor;

    @Schema(description = "Quilometragem mínima", example = "10000")
    private Integer minQuilometragem;

    @Schema(description = "Quilometragem máxima", example = "50000")
    private Integer maxQuilometragem;

    @Schema(description = "Status de ativação do veículo para venda", example = "true")
    private Boolean ativo;
}
