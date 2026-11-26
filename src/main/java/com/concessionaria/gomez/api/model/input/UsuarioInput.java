package com.concessionaria.gomez.api.model.input;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Cadastro de usuário. Não há campo de senha: o sistema gera uma senha temporária. */
@Getter
@Setter
public class UsuarioInput {

    @NotBlank
    @Size(max = 100)
    private String nome;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotNull
    private PerfilOperadorComercial perfil;
}
