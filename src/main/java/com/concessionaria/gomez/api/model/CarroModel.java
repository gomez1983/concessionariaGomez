package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CarroModel { //DTO

    private Long id;
    private String marca;
    private String modelo;
    private Integer ano;
    private BigDecimal compra;
    private OffsetDateTime dataCompra;
    private BigDecimal venda;
    private Boolean ativo;

    /*O método de soma da porcentagem da concessionária pode ser feito aqui
    ou pode ser criada uma nova classe DTO de Carro para isso.*/

    public BigDecimal getVenda() {
        if (this.compra == null) {
            return null;
        }
        return this.compra.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP);
    }
}
