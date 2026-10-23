package com.concessionaria.gomez.api.model.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VendaInput {

    @NotNull(message = "O ID do veículo é obrigatório")
    private Long carroId;

    @NotNull(message = "O ID do cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "O valor negociado da venda é obrigatório")
    @Positive(message = "O valor da venda deve ser positivo")
    private BigDecimal valorVenda;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres")
    private String observacoes;
}
