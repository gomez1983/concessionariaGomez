package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.api.model.input.CarroInput;
import com.concessionaria.gomez.domain.model.Carro;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CarroInputDisassembler {

    @Autowired
    private ModelMapper modelMapper;

    public Carro toDomainObject(CarroInput carroInput) {
        return modelMapper.map(carroInput, Carro.class);
    }
}