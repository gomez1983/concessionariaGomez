package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CarroModel {

    private Long id;
    private String marca;
    private String modelo;
    private Integer ano;
    private BigDecimal compra;
    private OffsetDateTime dataCompra;
    private BigDecimal venda;
    private Boolean ativo;

}
