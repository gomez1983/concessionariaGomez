package com.concessionaria.gomez.api.model.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecisaoAprovacaoInput {

    @NotNull(message = "A sequência da revisão sendo avaliada é obrigatória para controle de concorrência")
    private Integer sequenciaRevisao;

    @NotNull(message = "Informe se a proposta deve ser aprovada (true) ou rejeitada (false)")
    private Boolean aprovar;

    @Size(max = 255)
    private String motivo;
}
