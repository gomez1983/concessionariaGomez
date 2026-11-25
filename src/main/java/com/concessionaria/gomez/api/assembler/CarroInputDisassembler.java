package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.input.CarroInput;
import com.concessionaria.gomez.domain.model.Carro;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarroInputDisassembler {

    private final ModelMapper modelMapper;

    public Carro toDomainObject(CarroInput carroInput) {
        return modelMapper.map(carroInput, Carro.class);
    }

    public void copyToDomainObject(CarroInput carroInput, Carro carro) {
        modelMapper.map(carroInput, carro);
    }

    @Deprecated
    public void copyToDomaininObject(CarroInput carroInput, Carro carro) {
        copyToDomainObject(carroInput, carro);
    }
}
