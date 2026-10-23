package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.controller.CarroController;
import com.concessionaria.gomez.api.controller.CarroFotoController;
import com.concessionaria.gomez.api.model.FotoCarroModel;
import com.concessionaria.gomez.domain.model.FotoCarro;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class FotoCarroModelAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public FotoCarroModel toModel(FotoCarro fotoCarro) {
        if (fotoCarro == null) {
            return null;
        }

        FotoCarroModel fotoCarroModel = modelMapper.map(fotoCarro, FotoCarroModel.class);

        Long carroId = fotoCarro.getCarroId();

        if (carroId != null) {
            fotoCarroModel.add(linkTo(methodOn(CarroFotoController.class).buscar(carroId)).withSelfRel());
            fotoCarroModel.add(linkTo(methodOn(CarroController.class).buscar(carroId)).withRel("carro"));
        }

        return fotoCarroModel;
    }
}
