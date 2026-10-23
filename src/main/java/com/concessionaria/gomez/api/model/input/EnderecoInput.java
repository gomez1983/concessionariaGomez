package com.concessionaria.gomez.api.model.input;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnderecoInput {

    @Size(max = 9)
    private String cep;

    @Size(max = 100)
    private String logradouro;

    @Size(max = 20)
    private String numero;

    @Size(max = 60)
    private String complemento;

    @Size(max = 60)
    private String bairro;

    @Size(max = 60)
    private String cidade;

    @Size(min = 2, max = 2)
    private String estado;
}
