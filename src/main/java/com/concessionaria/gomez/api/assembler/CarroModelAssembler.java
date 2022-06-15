package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.domain.model.Carro;
import org.modelmapper.Converter;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CarroModelAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public CarroModel toModel (Carro carro) {
        return modelMapper.map(carro, CarroModel.class);
    }

    public List<CarroModel> toCollectionModel(List<Carro> carros) {
        return carros.stream()
                .map(carro -> toModel(carro))
                .collect(Collectors.toList());
    }
}