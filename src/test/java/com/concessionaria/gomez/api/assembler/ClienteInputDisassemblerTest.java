package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.input.ClienteInput;
import com.concessionaria.gomez.api.model.input.EnderecoInput;
import com.concessionaria.gomez.core.modelmapper.ModelMapperConfig;
import com.concessionaria.gomez.domain.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteInputDisassemblerTest {

    private ClienteInputDisassembler disassembler;

    @BeforeEach
    void setUp() {
        disassembler = new ClienteInputDisassembler();
        ModelMapperConfig config = new ModelMapperConfig();
        ReflectionTestUtils.setField(disassembler, "modelMapper", config.modelMapper());
    }

    @Test
    @DisplayName("Deve converter ClienteInput para entidade Cliente com endereço")
    void deveConverterInputParaEntidade() {
        ClienteInput input = new ClienteInput();
        input.setNome("Ana Paula");
        input.setCpfCnpj("12312312345");
        input.setEmail("ana@teste.com");
        input.setTelefone("(11) 98765-1234");

        EnderecoInput enderecoInput = new EnderecoInput();
        enderecoInput.setCep("04578-000");
        enderecoInput.setLogradouro("Avenida Berrini");
        enderecoInput.setNumero("500");
        enderecoInput.setCidade("São Paulo");
        enderecoInput.setEstado("SP");
        input.setEndereco(enderecoInput);

        Cliente cliente = disassembler.toDomainObject(input);

        assertThat(cliente).isNotNull();
        assertThat(cliente.getNome()).isEqualTo("Ana Paula");
        assertThat(cliente.getCpfCnpj()).isEqualTo("12312312345");
        assertThat(cliente.getEmail()).isEqualTo("ana@teste.com");
        assertThat(cliente.getEndereco()).isNotNull();
        assertThat(cliente.getEndereco().getCep()).isEqualTo("04578-000");
        assertThat(cliente.getEndereco().getCidade()).isEqualTo("São Paulo");
    }

    @Test
    @DisplayName("Deve copiar propriedades para cliente existente preservando id, ativo e dataCadastro")
    void deveCopiarParaObjetoExistentePreservandoCamposInternos() {
        OffsetDateTime dataCriacaoOriginal = OffsetDateTime.now().minusDays(5);

        Cliente clienteExistente = new Cliente();
        clienteExistente.setId(10L);
        clienteExistente.setNome("Carlos Original");
        clienteExistente.setCpfCnpj("11111111111");
        clienteExistente.setAtivo(true);
        clienteExistente.setDataCadastro(dataCriacaoOriginal);

        ClienteInput input = new ClienteInput();
        input.setNome("Carlos Atualizado");
        input.setCpfCnpj("11111111111");
        input.setTelefone("(11) 91111-2222");

        disassembler.copyToDomainObject(input, clienteExistente);

        assertThat(clienteExistente.getNome()).isEqualTo("Carlos Atualizado");
        assertThat(clienteExistente.getTelefone()).isEqualTo("(11) 91111-2222");
        assertThat(clienteExistente.getId()).isEqualTo(10L);
        assertThat(clienteExistente.getAtivo()).isTrue();
        assertThat(clienteExistente.getDataCadastro()).isEqualTo(dataCriacaoOriginal);
    }
}
