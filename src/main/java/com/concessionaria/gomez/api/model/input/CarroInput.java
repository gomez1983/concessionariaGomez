package com.concessionaria.gomez.api.model.input;
import javax.validation.constraints.NotBlank;

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

    @NotBlank
    private int ano;

    @NotBlank
    private double compra;

    private OffsetDateTime dataCompra;

}
