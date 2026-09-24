package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.input.CarroInput;
import com.concessionaria.gomez.core.modelmapper.ModelMapperConfig;
import com.concessionaria.gomez.domain.model.Carro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CarroInputDisassemblerTest {

    private CarroInputDisassembler disassembler;

    @BeforeEach
    void setUp() {
        disassembler = new CarroInputDisassembler();
        ModelMapperConfig config = new ModelMapperConfig();
        ReflectionTestUtils.setField(disassembler, "modelMapper", config.modelMapper());
    }

    @Test
    @DisplayName("Deve converter CarroInput para nova entidade Carro")
    void deveConverterInputParaEntidade() {
        CarroInput input = new CarroInput();
        input.setMarca("Honda");
        input.setModelo("Civic");
        input.setAno(2021);
        input.setCompra(new BigDecimal("120000.00"));

        Carro carro = disassembler.toDomainObject(input);

        assertThat(carro).isNotNull();
        assertThat(carro.getMarca()).isEqualTo("Honda");
        assertThat(carro.getModelo()).isEqualTo("Civic");
        assertThat(carro.getAno()).isEqualTo(2021);
        assertThat(carro.getCompra()).isEqualByComparingTo(new BigDecimal("120000.00"));
    }

    @Test
    @DisplayName("Deve copiar propriedades para entidade existente preservando campos não enviados como ativo e dataCompra")
    void deveCopiarParaObjetoDeDominioPreservandoCamposNaoInformados() {
        OffsetDateTime dataCriacaoOriginal = OffsetDateTime.now().minusDays(10);

        Carro carroExistente = new Carro();
        carroExistente.setId(1L);
        carroExistente.setMarca("Honda");
        carroExistente.setModelo("Civic");
        carroExistente.setAno(2020);
        carroExistente.setCompra(new BigDecimal("100000.00"));
        carroExistente.setAtivo(true);
        carroExistente.setDataCompra(dataCriacaoOriginal);

        CarroInput inputAtualizado = new CarroInput();
        inputAtualizado.setMarca("Honda");
        inputAtualizado.setModelo("Civic Touring");
        inputAtualizado.setAno(2021);
        inputAtualizado.setCompra(new BigDecimal("130000.00"));
        // dataCompra não é enviada (nula no DTO)

        disassembler.copyToDomainObject(inputAtualizado, carroExistente);

        // Campos atualizados
        assertThat(carroExistente.getModelo()).isEqualTo("Civic Touring");
        assertThat(carroExistente.getAno()).isEqualTo(2021);
        assertThat(carroExistente.getCompra()).isEqualByComparingTo(new BigDecimal("130000.00"));

        // Campos preservados intactos
        assertThat(carroExistente.getId()).isEqualTo(1L);
        assertThat(carroExistente.getAtivo()).isTrue();
        assertThat(carroExistente.getDataCompra()).isEqualTo(dataCriacaoOriginal);
    }
}
