package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.domain.model.Carro;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
        return carroModel;
    }

    public List<CarroModel> toCollectionModel(List<Carro> carros) {
        return carros.stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
}
