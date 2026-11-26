package com.concessionaria.gomez.api.model;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.Usuario;

import java.time.OffsetDateTime;

/** Usuário para a administração. Nunca expõe o hash da senha. */
public record UsuarioModel(
        Long id,
        String nome,
        String email,
        PerfilOperadorComercial perfil,
        Boolean ativo,
        OffsetDateTime bloqueadoAte,
        OffsetDateTime ultimoLogin,
        OffsetDateTime dataCadastro) {

    public static UsuarioModel de(Usuario usuario) {
        return new UsuarioModel(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getAtivo(),
                usuario.getBloqueadoAte(),
                usuario.getUltimoLogin(),
                usuario.getDataCadastro());
    }
}
