package com.concessionaria.gomez.api.model;

import java.time.Instant;

/** Resposta de login e de renovação. O refresh token vai só no cookie HttpOnly, nunca no corpo. */
public record TokenAcessoModel(
        String accessToken,
        String tokenType,
        Instant expiraEm,
        UsuarioLogadoModel usuario) {
}
