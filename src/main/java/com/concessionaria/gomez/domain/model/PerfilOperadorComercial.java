package com.concessionaria.gomez.domain.model;

/**
 * Perfil do operador do painel. Desde a Etapa 3a vem do JWT assinado pela API
 * (claim {@code perfil}); os antigos cabeçalhos {@code X-Operador-*} não são mais aceitos.
 */
public enum PerfilOperadorComercial {
    VENDEDOR,
    GERENTE,
    ADMIN;

    public boolean podeVerCustosEMargensInternas() {
        return this == GERENTE || this == ADMIN;
    }

    public boolean podeAprovarExcecaoGerencial() {
        return this == GERENTE || this == ADMIN;
    }

    public boolean podeConfigurarPolitica() {
        return this == ADMIN || this == GERENTE;
    }
}
