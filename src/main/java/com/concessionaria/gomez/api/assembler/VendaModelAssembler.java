package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.controller.CarroController;
import com.concessionaria.gomez.api.controller.ClienteController;
import com.concessionaria.gomez.api.controller.VendaController;
import com.concessionaria.gomez.api.model.VendaModel;
import com.concessionaria.gomez.domain.model.Venda;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class VendaModelAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public VendaModel toModel(Venda venda) {
        if (venda == null) {
            return null;
        }

        VendaModel vendaModel = modelMapper.map(venda, VendaModel.class);

        // HATEOAS: Link para o próprio recurso da venda (self)
        if (venda.getId() != null) {
            vendaModel.add(linkTo(methodOn(VendaController.class).buscar(venda.getId())).withSelfRel());
        }

        // HATEOAS: Link para a coleção geral de vendas
        vendaModel.add(linkTo(VendaController.class).withRel("vendas"));

        // HATEOAS: Link para o veículo vendido
        if (venda.getCarro() != null && venda.getCarro().getId() != null) {
            vendaModel.add(linkTo(methodOn(CarroController.class).buscar(venda.getCarro().getId())).withRel("carro"));
        }

        // HATEOAS: Link para o cliente comprador
        if (venda.getCliente() != null && venda.getCliente().getId() != null) {
            vendaModel.add(linkTo(methodOn(ClienteController.class).buscar(venda.getCliente().getId())).withRel("cliente"));
        }

        // HATEOAS: Ação para edição da venda
        if (venda.getId() != null) {
            vendaModel.add(linkTo(VendaController.class).slash(venda.getId()).withRel("editar"));
            vendaModel.add(linkTo(VendaController.class).slash(venda.getId()).withRel("cancelar"));
        }

        return vendaModel;
    }

    public List<VendaModel> toCollectionModel(List<Venda> vendas) {
        return vendas.stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
}
