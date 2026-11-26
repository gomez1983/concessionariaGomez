package com.concessionaria.gomez.api.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrocaSenhaInput {

    @NotBlank
    @Size(max = 72)
    private String senhaAtual;

    /** O limite de 72 bytes (acentos ocupam 2) é conferido pela PoliticaSenha no domínio. */
    @NotBlank
    @Size(min = 10, max = 72)
    private String novaSenha;
}
