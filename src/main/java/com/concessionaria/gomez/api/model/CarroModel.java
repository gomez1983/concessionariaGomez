package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarroModel { //DTO

    private Long id;
    private String marca;
    private String modelo;
    private int ano;
    private double compra;

}
    /*O método de soma da porcentagem da concessionária pode ser feito aqui
    ou pode ser criada uma nova classe DTO de Carro para isso.*/
