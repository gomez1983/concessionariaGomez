package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Relation(collectionRelation = "carros")
@Getter
@Setter
public class CarroModel extends RepresentationModel<CarroModel> {

    private Long id;
    private String marca;
    private String modelo;
    private Integer ano;
    private BigDecimal compra;
    private BigDecimal venda;
    private BigDecimal precoAnunciado;
    private Integer quilometragem;
    private Boolean zeroKm;
    private Boolean ativo;
    private String disponibilidadeComercial;
    private OffsetDateTime reservaValidaAte;
    private String reservaResponsavel;
    private OffsetDateTime dataCompra;
    private Long diasEmEstoque;

}
