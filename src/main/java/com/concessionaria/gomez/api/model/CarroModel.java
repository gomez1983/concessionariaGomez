package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CarroModel { //DTO

    private Long id;
    private String marca;
    private String modelo;
    private int ano;
    private double compra;
    private OffsetDateTime dataCompra;
    private double venda;
    private Boolean ativo;

    /*O método de soma da porcentagem da concessionária pode ser feito aqui
    ou pode ser criada uma nova classe DTO de Carro para isso.*/

    public double getVenda() {
        return Math.floor((this.compra * 1.10) * 100) / 100;
    }
}