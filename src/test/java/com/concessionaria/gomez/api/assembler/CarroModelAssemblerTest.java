package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.domain.model.Carro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarroModelAssemblerTest {

    private CarroModelAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new CarroModelAssembler();
        ReflectionTestUtils.setField(assembler, "modelMapper", new ModelMapper());
    }

    @Test
    @DisplayName("Deve converter Carro para CarroModel populando o valor de venda calculado pelo domínio")
    void deveConverterCarroParaCarroModelComValorVendaCalculado() {
        Carro carro = new Carro();
        carro.setId(1L);
        carro.setMarca("Toyota");
        carro.setModelo("Corolla");
        carro.setAno(2023);
        carro.setCompra(new BigDecimal("100000.00"));
        carro.setAtivo(true);

        CarroModel model = assembler.toModel(carro);

        assertThat(model).isNotNull();
        assertThat(model.getId()).isEqualTo(1L);
        assertThat(model.getMarca()).isEqualTo("Toyota");
        assertThat(model.getModelo()).isEqualTo("Corolla");
        assertThat(model.getCompra()).isEqualByComparingTo(new BigDecimal("100000.00"));
        // Margem de 10% calculada pelo domínio
        assertThat(model.getVenda()).isEqualByComparingTo(new BigDecimal("110000.00"));
    }

    @Test
    @DisplayName("Deve retornar null quando Carro for nulo")
    void deveRetornarNullQuandoCarroForNulo() {
        CarroModel model = assembler.toModel(null);
        assertThat(model).isNull();
    }

    @Test
    @DisplayName("Deve converter coleção de carros para coleção de modelos")
    void deveConverterColecaoDeCarros() {
        Carro c1 = new Carro();
        c1.setCompra(new BigDecimal("50000.00"));

        Carro c2 = new Carro();
        c2.setCompra(new BigDecimal("80000.00"));

        List<CarroModel> models = assembler.toCollectionModel(List.of(c1, c2));

        assertThat(models).hasSize(2);
        assertThat(models.get(0).getVenda()).isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(models.get(1).getVenda()).isEqualByComparingTo(new BigDecimal("88000.00"));
    }
}
