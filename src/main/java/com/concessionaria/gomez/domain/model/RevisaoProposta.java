package com.concessionaria.gomez.domain.model;

import com.concessionaria.gomez.domain.exception.NegocioException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "revisao_proposta",
        uniqueConstraints = @UniqueConstraint(name = "uk_revisao_proposta_seq", columnNames = {"proposta_id", "sequencia"})
)
public class RevisaoProposta {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id", nullable = false)
    private PropostaComercial proposta;

    @Column(nullable = false)
    private Integer sequencia;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "politica_comercial_id", nullable = false)
    private PoliticaComercial politicaComercial;

    @Column(name = "custo_aquisicao_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoAquisicaoSnapshot;

    @Column(name = "preco_anunciado_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoAnunciadoSnapshot;

    @Column(name = "preco_sugerido_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoSugeridoSnapshot;

    @Column(name = "preco_negociado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoNegociado;

    @Column(name = "custos_fixos_operacao", nullable = false, precision = 12, scale = 2)
    private BigDecimal custosFixosOperacao = BigDecimal.ZERO;

    @Column(name = "soma_taxas_proporcionais", nullable = false, precision = 10, scale = 6)
    private BigDecimal somaTaxasProporcionais = BigDecimal.ZERO;

    @Column(name = "receitas_adicionais_elegiveis", nullable = false, precision = 12, scale = 2)
    private BigDecimal receitasAdicionaisElegiveis = BigDecimal.ZERO;

    @Column(name = "contribuicao_estimada", nullable = false, precision = 12, scale = 2)
    private BigDecimal contribuicaoEstimada;

    @Column(name = "margem_contribuicao_estimada", nullable = false, precision = 10, scale = 6)
    private BigDecimal margemContribuicaoEstimada;

    @Column(name = "desconto_reais", nullable = false, precision = 12, scale = 2)
    private BigDecimal descontoReais;

    @Column(name = "desconto_percentual", nullable = false, precision = 10, scale = 6)
    private BigDecimal descontoPercentual;

    @Column(name = "markup_aquisicao", nullable = false, precision = 10, scale = 6)
    private BigDecimal markupAquisicao;

    @Column(name = "piso_autonomia_calculado", nullable = false, precision = 12, scale = 2)
    private BigDecimal pisoAutonomiaCalculado;

    @Column(name = "piso_absoluto_calculado", nullable = false, precision = 12, scale = 2)
    private BigDecimal pisoAbsolutoCalculado;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcada_analise", nullable = false, length = 40)
    private AlcadaAnaliseFinanceira alcadaAnalise;

    @Column(name = "possui_custos_pendentes", nullable = false)
    private Boolean possuiCustosPendentes = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean submetida = Boolean.FALSE;

    @Column(name = "justificativa_vendedor", length = 255)
    private String justificativaVendedor;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, columnDefinition = "datetime", updatable = false)
    private OffsetDateTime dataCriacao;

    @OneToMany(mappedBy = "revisao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemFinanceiroRevisao> itensFinanceiros = new ArrayList<>();

    @OneToMany(mappedBy = "revisao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagamentoProposta> pagamentos = new ArrayList<>();

    @OneToOne(mappedBy = "revisao", cascade = CascadeType.ALL, orphanRemoval = true)
    private FinanciamentoProposta financiamento;

    @OneToOne(mappedBy = "revisao", cascade = CascadeType.ALL, orphanRemoval = true)
    private AvaliacaoTroca avaliacaoTroca;

    @OneToMany(mappedBy = "revisao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DecisaoAprovacao> decisoes = new ArrayList<>();

    public void submeter() {
        if (Boolean.TRUE.equals(this.submetida)) {
            throw new NegocioException("Esta revisão da proposta já foi submetida e é imutável.");
        }
        this.submetida = Boolean.TRUE;
    }

    public void garantirMutavel() {
        if (Boolean.TRUE.equals(this.submetida)) {
            throw new NegocioException("Revisão submetida é imutável; crie uma nova revisão para alterar condições comerciais.");
        }
    }
}
