package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.assembler.CarroInputDisassembler;
import com.concessionaria.gomez.api.assembler.CarroModelAssembler;
import com.concessionaria.gomez.api.model.CarroModel;
import com.concessionaria.gomez.domain.filter.CarroFilter;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarroControllerTest {

    @Mock
    private CarroRepository carroRepository;

    @Mock
    private CadastroCarroService cadastroCarro;

    @Mock
    private CarroModelAssembler carroModelAssembler;

    @Mock
    private CarroInputDisassembler carroInputDisassembler;

    @InjectMocks
    private CarroController carroController;

    @Test
    @DisplayName("Deve retornar página de CarroModel preservando paginação e metadados")
    void deveRetornarPaginaDeCarroModelComMetadados() {
        Carro carro1 = new Carro();
        carro1.setId(1L);
        carro1.setMarca("BMW");
        carro1.setModelo("M3");
        carro1.setAno(2021);
        carro1.setCompra(new BigDecimal("250000.00"));
        carro1.setQuilometragem(15000);

        CarroModel model1 = new CarroModel();
        model1.setId(1L);
        model1.setMarca("BMW");
        model1.setModelo("M3");
        model1.setAno(2021);
        model1.setCompra(new BigDecimal("250000.00"));
        model1.setVenda(new BigDecimal("275000.00"));
        model1.setQuilometragem(15000);
        model1.setZeroKm(false);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("compra").descending());
        Page<Carro> paginaCarros = new PageImpl<>(List.of(carro1), pageable, 1);

        when(carroRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(paginaCarros);
        when(carroModelAssembler.toModel(carro1)).thenReturn(model1);

        Page<CarroModel> resultado = carroController.listar(new CarroFilter(), pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getTotalPages());
        assertEquals(10, resultado.getSize());
        assertEquals(0, resultado.getNumber());
        assertEquals(1, resultado.getContent().size());
        assertEquals("BMW", resultado.getContent().getFirst().getMarca());
        assertEquals(new BigDecimal("275000.00"), resultado.getContent().getFirst().getVenda());
        assertEquals(15000, resultado.getContent().getFirst().getQuilometragem());
        assertEquals(false, resultado.getContent().getFirst().getZeroKm());

        verify(carroRepository).findAll(any(Specification.class), eq(pageable));
        verify(carroModelAssembler).toModel(carro1);
    }

    @Test
    @DisplayName("Deve repassar filtros dinâmicos via Specification para o repository junto com Pageable (incluindo range de km)")
    void deveRepassarFiltrosCompletosComPageable() {
        CarroFilter filtro = new CarroFilter();
        filtro.setMarca("Mercedes");
        filtro.setAno(2023);
        filtro.setMinValor(new BigDecimal("100000.00"));
        filtro.setMaxValor(new BigDecimal("300000.00"));
        filtro.setMinQuilometragem(10000);
        filtro.setMaxQuilometragem(60000);
        filtro.setAtivo(true);

        Pageable pageable = PageRequest.of(1, 5, Sort.by("ano").ascending());

        Page<Carro> paginaVazia = new PageImpl<>(List.of(), pageable, 0);
        when(carroRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaVazia);

        Page<CarroModel> resultado = carroController.listar(filtro, pageable);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        assertEquals(0, resultado.getTotalElements());
        assertEquals(5, resultado.getSize());
        assertEquals(1, resultado.getNumber());

        verify(carroRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoInteractions(carroModelAssembler);
    }
}
