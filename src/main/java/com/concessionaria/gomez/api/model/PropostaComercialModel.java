package com.concessionaria.gomez.api.model;

import com.concessionaria.gomez.domain.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Relation(collectionRelation = "propostas")
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropostaComercialModel extends RepresentationModel<PropostaComercialModel> {

    private Long id;
    private String numero;
    private CarroResumoModel carro;
    private ClienteResumoModel cliente;
    private String vendedorIdentificador;
    private StatusPropostaComercial status;
    private Integer revisaoAtualSequencia;
    private OffsetDateTime validaAte;
    private OffsetDateTime dataCriacao;
    private RevisaoPropostaModel revisaoAtual;
    private ResumoClienteModel resumoParaCliente;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RevisaoPropostaModel {
        private Long id;
        private Integer sequencia;
        private BigDecimal precoAnunciado;
        private BigDecimal precoSugerido;
        private BigDecimal precoNegociado;
        private BigDecimal descontoReais;
        private BigDecimal descontoPercentual;
        private AlcadaAnaliseFinanceira alcadaAnalise;
        private Boolean possuiCustosPendentes;
        private Boolean submetida;
        private String justificativaVendedor;
        private OffsetDateTime dataCriacao;

        // Campos internos sensíveis: exibidos apenas para GERENTE e ADMIN
        private BigDecimal custoAquisicao;
        private BigDecimal custosFixosOperacao;
        private BigDecimal somaTaxasProporcionais;
        private BigDecimal receitasAdicionaisElegiveis;
        private BigDecimal contribuicaoEstimada;
        private BigDecimal margemContribuicaoEstimada;
        private BigDecimal markupAquisicao;
        private BigDecimal pisoAutonomia;
        private BigDecimal pisoAbsoluto;

        private AvaliacaoTrocaModel avaliacaoTroca;
        private FinanciamentoModel financiamento;
        private List<PagamentoModel> pagamentos;
        private List<DecisaoModel> decisoes;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AvaliacaoTrocaModel {
        private String identificacaoVeiculoUsado;
        private BigDecimal valorAvaliacaoTecnica;
        private BigDecimal valorComercialAceito;
        private BigDecimal saldoQuitacaoVerificado;
        private BigDecimal creditoLiquido;
        private Boolean possuiPendenciaDocumental;
        private String descricaoPendencias;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FinanciamentoModel {
        private String instituicaoFinanceira;
        private BigDecimal principalFinanciado;
        private BigDecimal entradaMonetariaRelacionada;
        private Integer prazoMeses;
        private BigDecimal taxaMensal;
        private BigDecimal cetAnualInformado;
        private BigDecimal valorParcela;
        private Boolean apenasEstimativa;
        private String situacaoExterna;
    }

    @Getter
    @Builder
    public static class PagamentoModel {
        private ModalidadePagamentoProposta modalidade;
        private BigDecimal valor;
        private String observacao;
    }

    @Getter
    @Builder
    public static class DecisaoModel {
        private String decisorAutenticado;
        private ResultadoDecisaoAprovacao resultado;
        private AlcadaAnaliseFinanceira alcada;
        private String motivo;
        private OffsetDateTime dataDecisao;
    }

    @Getter
    @Builder
    public static class ResumoClienteModel {
        private String numeroProposta;
        private String veiculo;
        private String cliente;
        private BigDecimal precoAnunciado;
        private BigDecimal descontoConcedido;
        private BigDecimal precoFinalProposto;
        private BigDecimal creditoLiquidoUsadoNaTroca;
        private BigDecimal valorFinanciado;
        private Integer prazoFinanciamentoMeses;
        private OffsetDateTime validaAte;
    }
}
