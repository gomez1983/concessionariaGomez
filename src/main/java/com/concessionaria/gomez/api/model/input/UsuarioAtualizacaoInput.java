package com.concessionaria.gomez.api.model.input;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Alteração de usuário. O e-mail não muda: ele identifica o autor nos registros de auditoria. */
@Getter
@Setter
public class UsuarioAtualizacaoInput {

    @NotBlank
    @Size(max = 100)
    private String nome;

    @NotNull
    private PerfilOperadorComercial perfil;
}
