package com.concessionaria.gomez.api.model.input;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CarroInput {

    @NotBlank
    private String marca;

    @NotBlank
    private String modelo;

    @NotNull
    private int ano;

    @NotNull
    private double compra;

    @NotNull
    private OffsetDateTime dataCompra;
}