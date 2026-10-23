package com.concessionaria.gomez.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteTest {

    @Test
    @DisplayName("Cliente novo deve nascer ativo por padrão")
    void deveNascerAtivoPorPadrao() {
        Cliente cliente = new Cliente();
        assertThat(cliente.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve inativar cliente corretamente")
    void deveInativarCliente() {
        Cliente cliente = new Cliente();
        cliente.inativar();
        assertThat(cliente.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve reativar cliente inativado")
    void deveAtivarCliente() {
        Cliente cliente = new Cliente();
        cliente.inativar();
        assertThat(cliente.getAtivo()).isFalse();

        cliente.ativar();
        assertThat(cliente.getAtivo()).isTrue();
    }
}
