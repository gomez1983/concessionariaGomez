package com.concessionaria.gomez.api.model.input;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.concessionaria.gomez.api.validation.ImagemValida;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class FotoCarroInput {

    @Schema(description = "Arquivo da foto do veículo (JPEG, PNG ou WebP)")
    @NotNull(message = "O arquivo de imagem é obrigatório")
    @ImagemValida
    private MultipartFile arquivo;

    @Schema(description = "Descrição detalhada da foto (ex: Frente, Lateral, Interior)")
    @NotBlank(message = "A descrição da foto é obrigatória")
    @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres")
    private String descricao;
}
