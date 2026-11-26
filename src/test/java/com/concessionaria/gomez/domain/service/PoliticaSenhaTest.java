package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaSenhaTest {

    @Test
    @DisplayName("Aceita de 10 a 72 caracteres, limitado a 72 bytes em UTF-8")
    void limites() {
        assertThat(PoliticaSenha.ehValida("a".repeat(10))).isTrue();
        assertThat(PoliticaSenha.ehValida("a".repeat(72))).isTrue();
        assertThat(PoliticaSenha.ehValida("a".repeat(9))).isFalse();
        assertThat(PoliticaSenha.ehValida("a".repeat(73))).isFalse();
        assertThat(PoliticaSenha.ehValida("ç".repeat(36))).isTrue();   // 72 bytes
        assertThat(PoliticaSenha.ehValida("ç".repeat(37))).isFalse();  // 74 bytes, só 37 caracteres
        assertThat(PoliticaSenha.ehValida(" ".repeat(12))).isFalse();
        assertThat(PoliticaSenha.ehValida(null)).isFalse();
    }

    @Test
    @DisplayName("validar lança NegocioException (400) com a mensagem da política")
    void validar() {
        assertThatThrownBy(() -> PoliticaSenha.validar("curta"))
                .isInstanceOf(NegocioException.class)
                .hasMessage(PoliticaSenha.MSG_SENHA_INVALIDA);
        assertThatCode(() -> PoliticaSenha.validar("senha-valida-123")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Senha temporária: 16 caracteres sem ambíguos, válida pela política e diferente a cada geração")
    void senhaTemporaria() {
        Set<String> geradas = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String senha = PoliticaSenha.gerarTemporaria();
            assertThat(senha).hasSize(16).doesNotContainPattern("[0O1lI]");
            assertThat(PoliticaSenha.ehValida(senha)).isTrue();
            geradas.add(senha);
        }
        assertThat(geradas).hasSize(200);
    }
}
