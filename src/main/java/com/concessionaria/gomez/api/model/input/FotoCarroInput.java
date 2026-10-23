package com.concessionaria.gomez.api.model.input;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class FotoCarroInput {

    @Schema(description = "Arquivo da foto do veículo (JPEG ou PNG)")
    @NotNull(message = "O arquivo de imagem é obrigatório")
    private MultipartFile arquivo;

    @Schema(description = "Descrição detalhada da foto (ex: Frente, Lateral, Interior)")
    @NotBlank(message = "A descrição da foto é obrigatória")
    private String descricao;
}
