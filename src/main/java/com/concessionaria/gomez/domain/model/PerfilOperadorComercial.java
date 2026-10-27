package com.concessionaria.gomez.domain.model;

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

    public static PerfilOperadorComercial fromHeader(String raw) {
        if (raw == null || raw.isBlank()) {
            return GERENTE;
        }
        try {
            return PerfilOperadorComercial.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return VENDEDOR;
        }
    }
}
