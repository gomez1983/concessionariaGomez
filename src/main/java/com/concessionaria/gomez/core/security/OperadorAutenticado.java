package com.concessionaria.gomez.core.security;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;

/**
 * Operador da requisição, extraído do JWT validado. Substitui os antigos cabeçalhos
 * {@code X-Operador-Perfil} e {@code X-Operador-Id}, que o cliente podia forjar.
 * Declare como parâmetro de um método de controller para recebê-lo.
 */
public record OperadorAutenticado(String email, String nome, PerfilOperadorComercial perfil) {

    public boolean ehGestao() {
        return perfil.podeAprovarExcecaoGerencial();
    }
}
