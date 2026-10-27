package com.concessionaria.gomez.api.model.input;

import com.concessionaria.gomez.domain.model.BaseCalculoItemFinanceiro;
import com.concessionaria.gomez.domain.model.ModalidadePagamentoProposta;
import com.concessionaria.gomez.domain.model.NaturezaItemFinanceiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PropostaComercialInput {

    @NotNull(message = "O ID do veículo é obrigatório")
    private Long carroId;

    @NotNull(message = "O ID do cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "O preço negociado da proposta é obrigatório")
    @Positive(message = "O preço negociado deve ser positivo")
    private BigDecimal precoNegociado;

    private String justificativaVendedor;

    private boolean submeter = false;

    @Valid
    private List<ItemFinanceiroInput> itensExtras = new ArrayList<>();

    @Valid
    private List<PagamentoInput> pagamentos = new ArrayList<>();

    @Valid
    private FinanciamentoInput financiamento;

    @Valid
    private AvaliacaoTrocaInput avaliacaoTroca;

    private List<String> codigosCampanha = new ArrayList<>();

    @Getter
    @Setter
    public static class ItemFinanceiroInput {
        @NotNull
        private NaturezaItemFinanceiro natureza;
        @NotNull
        private BaseCalculoItemFinanceiro baseCalculo = BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS;
        @PositiveOrZero
        private BigDecimal valor;
        private boolean confirmado = true;
        @NotBlank
        private String descricao;
    }

    @Getter
    @Setter
    public static class PagamentoInput {
        @NotNull
        private ModalidadePagamentoProposta modalidade;
        @NotNull
        @Positive
        private BigDecimal valor;
        private String observacao;
    }

    @Getter
    @Setter
    public static class FinanciamentoInput {
        @NotBlank
        private String instituicaoFinanceira;
        @NotNull
        @Positive
        private BigDecimal principalFinanciado;
        @PositiveOrZero
        private BigDecimal entradaMonetariaRelacionada = BigDecimal.ZERO;
        @NotNull
        @Positive
        private Integer prazoMeses;
        @PositiveOrZero
        private BigDecimal taxaMensal;
        @PositiveOrZero
        private BigDecimal cetAnualInformado;
        @PositiveOrZero
        private BigDecimal valorParcela;
        private boolean apenasEstimativa = true;
        private String situacaoExterna = "SIMULADO";
    }

    @Getter
    @Setter
    public static class AvaliacaoTrocaInput {
        @NotBlank
        private String identificacaoVeiculoUsado;
        @NotNull
        @Positive
        private BigDecimal valorAvaliacaoTecnica;
        @NotNull
        @Positive
        private BigDecimal valorComercialAceito;
        @PositiveOrZero
        private BigDecimal saldoQuitacaoVerificado = BigDecimal.ZERO;
        private boolean possuiPendenciaDocumental = false;
        private String descricaoPendencias;
    }
}
