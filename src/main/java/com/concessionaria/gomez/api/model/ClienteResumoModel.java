package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteResumoModel {

    private Long id;
    private String nome;
    private String cpfCnpj;
}
