package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.controller.CarroController;
import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.domain.model.Carro;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class CarroModelAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public CarroModel toModel(Carro carro) {
        if (carro == null) {
            return null;
        }

        CarroModel carroModel = modelMapper.map(carro, CarroModel.class);
        carroModel.setVenda(carro.calcularPrecoVendaSugerido());
        carroModel.setZeroKm(carro.isZeroKm());

        // HATEOAS: Link para o próprio recurso individual (self) -> GET /carros/{id}
        if (carro.getId() != null) {
            carroModel.add(linkTo(methodOn(CarroController.class).buscar(carro.getId())).withSelfRel());
        }

        // HATEOAS: Link para a coleção geral de carros -> GET /carros
        carroModel.add(linkTo(CarroController.class).withRel("carros"));

        // HATEOAS: Links dinâmicos de transição de estado de negócio (Ativar / Inativar) -> /carros/{id}/ativo
        if (carro.getId() != null) {
            if (Boolean.TRUE.equals(carro.getAtivo())) {
                carroModel.add(linkTo(CarroController.class)
                        .slash(carro.getId())
                        .slash("ativo")
                        .withRel("inativar"));
            } else {
                carroModel.add(linkTo(CarroController.class)
                        .slash(carro.getId())
                        .slash("ativo")
                        .withRel("ativar"));
            }
        }

        return carroModel;
    }

    public List<CarroModel> toCollectionModel(List<Carro> carros) {
        return carros.stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
}
