package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter
@Setter
public class FotoGaleriaModel extends RepresentationModel<FotoGaleriaModel> {
    private Long id;
    private String descricao;
    private String contentType;
    private Long tamanho;
}
