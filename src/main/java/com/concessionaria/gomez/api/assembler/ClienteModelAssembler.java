package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.controller.ClienteController;
import com.concessionaria.gomez.api.model.ClienteModel;
import com.concessionaria.gomez.domain.model.Cliente;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ClienteModelAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public ClienteModel toModel(Cliente cliente) {
        if (cliente == null) {
            return null;
        }

        ClienteModel clienteModel = modelMapper.map(cliente, ClienteModel.class);

        // HATEOAS: Link para o próprio recurso individual (self) -> GET /clientes/{id}
        if (cliente.getId() != null) {
            clienteModel.add(linkTo(methodOn(ClienteController.class).buscar(cliente.getId())).withSelfRel());
        }

        // HATEOAS: Link para a coleção geral de clientes -> GET /clientes
        clienteModel.add(linkTo(ClienteController.class).withRel("clientes"));

        // HATEOAS: Links dinâmicos de transição de estado de negócio (Ativar / Inativar) -> /clientes/{id}/ativo
        if (cliente.getId() != null) {
            if (Boolean.TRUE.equals(cliente.getAtivo())) {
                clienteModel.add(linkTo(ClienteController.class)
                        .slash(cliente.getId())
                        .slash("ativo")
                        .withRel("inativar"));
            } else {
                clienteModel.add(linkTo(ClienteController.class)
                        .slash(cliente.getId())
                        .slash("ativo")
                        .withRel("ativar"));
            }
        }

        return clienteModel;
    }

    public List<ClienteModel> toCollectionModel(List<Cliente> clientes) {
        return clientes.stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
}
