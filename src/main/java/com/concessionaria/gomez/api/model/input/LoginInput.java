package com.concessionaria.gomez.api.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class LoginInput {

    @NotBlank
    @Size(max = 100)
    private String email;

    @ToString.Exclude
    @NotBlank
    @Size(max = 72)
    private String senha;
}
