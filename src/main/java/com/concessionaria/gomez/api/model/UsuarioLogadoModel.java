package com.concessionaria.gomez.api.model;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;

public record UsuarioLogadoModel(String nome, String email, PerfilOperadorComercial perfil) {
}
