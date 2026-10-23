package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.input.ClienteInput;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.model.Endereco;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClienteInputDisassembler {

    @Autowired
    private ModelMapper modelMapper;

    public Cliente toDomainObject(ClienteInput clienteInput) {
        return modelMapper.map(clienteInput, Cliente.class);
    }

    public void copyToDomainObject(ClienteInput clienteInput, Cliente cliente) {
        if (clienteInput.getEndereco() != null && cliente.getEndereco() == null) {
            cliente.setEndereco(new Endereco());
        }
        modelMapper.map(clienteInput, cliente);
    }
}
