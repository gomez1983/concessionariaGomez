package com.concessionaria.gomez.api.model.input;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CarroInput {

    @NotBlank // Para objetos que sejam do tipo String, deve-se utilizar NotBlank
    private String marca;

    @NotBlank // Para objetos que sejam do tipo String, deve-se utilizar NotBlank
    private String modelo;

    @NotNull // Para objetos que não sejam do tipo String, deve-se utilizar NotNUll
    private Integer ano; //Utilizar o Integer (classe) no lugar de int

    @NotNull // Para objetos que não sejam do tipo String, deve-se utilizar NotNUll
    @PositiveOrZero
    private BigDecimal compra;

    private OffsetDateTime dataCompra;
}
