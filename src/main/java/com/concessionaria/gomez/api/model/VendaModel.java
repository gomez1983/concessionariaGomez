package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Relation(collectionRelation = "vendas")
@Getter
@Setter
public class VendaModel extends RepresentationModel<VendaModel> {

    private Long id;
    private CarroResumoModel carro;
    private ClienteResumoModel cliente;
    private BigDecimal valorVenda;
    private OffsetDateTime dataVenda;
    private String observacoes;
    private Boolean origemLegada;
    private Boolean cancelada;
    private String motivoCancelamento;
}
