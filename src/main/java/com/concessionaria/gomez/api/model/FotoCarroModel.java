package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "fotos")
@Getter
@Setter
public class FotoCarroModel extends RepresentationModel<FotoCarroModel> {

    private String nomeArquivo;
    private String descricao;
    private String contentType;
    private Long tamanho;
}
