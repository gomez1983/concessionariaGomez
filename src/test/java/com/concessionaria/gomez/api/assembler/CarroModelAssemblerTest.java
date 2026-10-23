package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.domain.model.Carro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarroModelAssemblerTest {

    private CarroModelAssembler assembler;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assembler = new CarroModelAssembler();
        ReflectionTestUtils.setField(assembler, "modelMapper", new ModelMapper());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Deve converter Carro para CarroModel populando valor de venda, quilometragem, zeroKm e links HATEOAS")
    void deveConverterCarroParaCarroModelComValorVendaCalculadoELinksHateoas() {
        Carro carro = new Carro();
        carro.setId(1L);
        carro.setMarca("Toyota");
        carro.setModelo("Corolla");
        carro.setAno(2023);
        carro.setCompra(new BigDecimal("100000.00"));
        carro.setQuilometragem(0);
        carro.setAtivo(true);

        CarroModel model = assembler.toModel(carro);

        assertThat(model).isNotNull();
        assertThat(model.getId()).isEqualTo(1L);
        assertThat(model.getMarca()).isEqualTo("Toyota");
        assertThat(model.getModelo()).isEqualTo("Corolla");
        assertThat(model.getCompra()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(model.getVenda()).isEqualByComparingTo(new BigDecimal("110000.00"));
        assertThat(model.getQuilometragem()).isEqualTo(0);
        assertThat(model.getZeroKm()).isTrue();

        // Verificando os links HATEOAS (Richardson Nível 3)
        assertThat(model.hasLink("self")).isTrue();
        assertThat(model.getLink("self").orElseThrow().getHref()).endsWith("/carros/1");

        assertThat(model.hasLink("carros")).isTrue();
        assertThat(model.getLink("carros").orElseThrow().getHref()).endsWith("/carros");

        assertThat(model.hasLink("inativar")).isTrue();
        assertThat(model.getLink("inativar").orElseThrow().getHref()).endsWith("/carros/1/ativo");
    }

    @Test
    @DisplayName("Deve gerar link de ativar quando o carro estiver inativo e mapear quilometragem usada")
    void deveGerarLinkAtivarQuandoCarroInativo() {
        Carro carro = new Carro();
        carro.setId(2L);
        carro.setMarca("Honda");
        carro.setModelo("Civic");
        carro.setAno(2020);
        carro.setCompra(new BigDecimal("90000.00"));
        carro.setQuilometragem(45000);
        carro.setAtivo(false);

        CarroModel model = assembler.toModel(carro);

        assertThat(model).isNotNull();
        assertThat(model.getQuilometragem()).isEqualTo(45000);
        assertThat(model.getZeroKm()).isFalse();
        assertThat(model.hasLink("ativar")).isTrue();
        assertThat(model.hasLink("inativar")).isFalse();
        assertThat(model.getLink("ativar").orElseThrow().getHref()).endsWith("/carros/2/ativo");
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
        c1.setId(10L);
        c1.setCompra(new BigDecimal("50000.00"));
        c1.setQuilometragem(20000);
        c1.setAtivo(true);

        Carro c2 = new Carro();
        c2.setId(11L);
        c2.setCompra(new BigDecimal("80000.00"));
        c2.setQuilometragem(0);
        c2.setAtivo(false);

        List<CarroModel> models = assembler.toCollectionModel(List.of(c1, c2));

        assertThat(models).hasSize(2);
        assertThat(models.get(0).getVenda()).isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(models.get(0).getZeroKm()).isFalse();
        assertThat(models.get(1).getVenda()).isEqualByComparingTo(new BigDecimal("88000.00"));
        assertThat(models.get(1).getZeroKm()).isTrue();
        assertThat(models.get(0).hasLink("self")).isTrue();
        assertThat(models.get(1).hasLink("self")).isTrue();
    }
}
