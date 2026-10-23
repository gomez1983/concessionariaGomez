package com.concessionaria.gomez.api.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotNull(message = "A quilometragem é obrigatória")
    @PositiveOrZero(message = "A quilometragem não pode ser negativa")
    private Integer quilometragem;

    @PastOrPresent
    private OffsetDateTime dataCompra;
}
