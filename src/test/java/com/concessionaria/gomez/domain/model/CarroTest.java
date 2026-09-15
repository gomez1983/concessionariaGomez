package com.concessionaria.gomez.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CarroTest {

    @Test
    @DisplayName("Deve calcular preço de venda sugerido aplicando margem padrão de 10%")
    void deveCalcularPrecoVendaSugeridoComMargemPadrao() {
        Carro carro = new Carro();
        carro.setCompra(new BigDecimal("100000.00"));

        BigDecimal vendaSugerida = carro.calcularPrecoVendaSugerido();

        assertThat(vendaSugerida).isEqualByComparingTo(new BigDecimal("110000.00"));
    }

    @Test
    @DisplayName("Deve calcular preço de venda com margem customizada informada")
    void deveCalcularPrecoVendaComMargemCustomizada() {
        Carro carro = new Carro();
        carro.setCompra(new BigDecimal("50000.00"));

        BigDecimal vendaSugerida = carro.calcularPrecoVendaSugerido(new BigDecimal("0.20"));

        assertThat(vendaSugerida).isEqualByComparingTo(new BigDecimal("60000.00"));
    }

    @Test
    @DisplayName("Deve retornar nulo quando o valor de compra não for informado")
    void deveRetornarNullQuandoCompraForNula() {
        Carro carro = new Carro();
        carro.setCompra(null);

        BigDecimal vendaSugerida = carro.calcularPrecoVendaSugerido();

        assertThat(vendaSugerida).isNull();
    }

    @Test
    @DisplayName("Deve arredondar com HALF_UP e manter 2 casas decimais no cálculo financeiro")
    void deveArredondarCorretamenteCasasDecimais() {
        Carro carro = new Carro();
        carro.setCompra(new BigDecimal("319900.55"));

        // 319900.55 * 1.10 = 351890.605 -> HALF_UP -> 351890.61
        BigDecimal vendaSugerida = carro.calcularPrecoVendaSugerido();

        assertThat(vendaSugerida).isEqualByComparingTo(new BigDecimal("351890.61"));
    }
}
