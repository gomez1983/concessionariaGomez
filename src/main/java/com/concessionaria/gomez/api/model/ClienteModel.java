package com.concessionaria.gomez.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;

@Relation(collectionRelation = "clientes")
@Getter
@Setter
public class ClienteModel extends RepresentationModel<ClienteModel> {

    private Long id;
    private String nome;
    private String cpfCnpj;
    private String telefone;
    private String email;
    private Boolean ativo;
    private OffsetDateTime dataCadastro;
    private EnderecoModel endereco;
}
