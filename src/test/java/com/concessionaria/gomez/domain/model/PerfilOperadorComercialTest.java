package com.concessionaria.gomez.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilOperadorComercialTest {

    @Test
    @DisplayName("VENDEDOR não vê custos, não aprova exceções e não configura política")
    void vendedorNaoTemPermissoesGerenciais() {
        PerfilOperadorComercial vendedor = PerfilOperadorComercial.VENDEDOR;

        assertThat(vendedor.podeVerCustosEMargensInternas()).isFalse();
        assertThat(vendedor.podeAprovarExcecaoGerencial()).isFalse();
        assertThat(vendedor.podeConfigurarPolitica()).isFalse();
    }

    @Test
    @DisplayName("GERENTE e ADMIN têm as permissões gerenciais")
    void gerenteEAdminTemPermissoesGerenciais() {
        for (PerfilOperadorComercial perfil : new PerfilOperadorComercial[]{
                PerfilOperadorComercial.GERENTE, PerfilOperadorComercial.ADMIN}) {
            assertThat(perfil.podeVerCustosEMargensInternas()).isTrue();
            assertThat(perfil.podeAprovarExcecaoGerencial()).isTrue();
            assertThat(perfil.podeConfigurarPolitica()).isTrue();
        }
    }
}
