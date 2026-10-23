package com.concessionaria.gomez.infrastructure.repository.spec;

import com.concessionaria.gomez.domain.filter.CarroFilter;
import com.concessionaria.gomez.domain.model.Carro;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CarroSpecsTest {

    @Mock
    private Root<Carro> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder builder;

    @Mock
    private Path<Object> path;

    @Mock
    private Expression<String> expression;

    @Mock
    private Predicate predicate;

    @Test
    @DisplayName("Deve construir predicados dinâmicos quando todos os filtros forem fornecidos (incluindo quilometragem)")
    void deveConstruirPredicadosComFiltroCompleto() {
        CarroFilter filtro = new CarroFilter();
        filtro.setMarca("BMW");
        filtro.setModelo("M3");
        filtro.setAno(2022);
        filtro.setMinValor(new BigDecimal("100000.00"));
        filtro.setMaxValor(new BigDecimal("300000.00"));
        filtro.setMinQuilometragem(10000);
        filtro.setMaxQuilometragem(50000);
        filtro.setAtivo(true);

        when(root.<Object>get(anyString())).thenReturn(path);
        when(builder.lower(any())).thenReturn(expression);
        when(builder.like(any(), anyString())).thenReturn(predicate);
        when(builder.equal(any(), any())).thenReturn(predicate);
        when(builder.greaterThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);
        when(builder.lessThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);
        when(builder.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        when(builder.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        Specification<Carro> spec = CarroSpecs.usandoFiltro(filtro);
        Predicate result = spec.toPredicate(root, query, builder);

        assertNotNull(result);
        verify(builder, atLeastOnce()).like(any(), eq("%bmw%"));
        verify(builder, atLeastOnce()).like(any(), eq("%m3%"));
        verify(builder, atLeastOnce()).equal(any(), eq(2022));
        verify(builder, atLeastOnce()).equal(any(), eq(true));
        verify(builder, atLeastOnce()).greaterThanOrEqualTo(any(), eq(new BigDecimal("100000.00")));
        verify(builder, atLeastOnce()).lessThanOrEqualTo(any(), eq(new BigDecimal("300000.00")));
        verify(builder, atLeastOnce()).greaterThanOrEqualTo(any(), eq(10000));
        verify(builder, atLeastOnce()).lessThanOrEqualTo(any(), eq(50000));
    }

    @Test
    @DisplayName("Deve retornar predicado vazio sem erros quando filtro for nulo")
    void deveRetornarPredicadoVazioQuandoFiltroNulo() {
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        Specification<Carro> spec = CarroSpecs.usandoFiltro(null);
        Predicate result = spec.toPredicate(root, query, builder);

        assertNotNull(result);
        verify(builder).and(any(Predicate[].class));
    }
}
