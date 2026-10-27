package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "financiamento_proposta")
public class FinanciamentoProposta {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "revisao_id", nullable = false, unique = true)
    private RevisaoProposta revisao;

    @Column(name = "instituicao_financeira", nullable = false, length = 100)
    private String instituicaoFinanceira;

    @Column(name = "principal_financiado", nullable = false, precision = 12, scale = 2)
    private BigDecimal principalFinanciado;

    @Column(name = "entrada_monetaria_relacionada", nullable = false, precision = 12, scale = 2)
    private BigDecimal entradaMonetariaRelacionada = BigDecimal.ZERO;

    @Column(name = "prazo_meses", nullable = false)
    private Integer prazoMeses;

    @Column(name = "taxa_mensal", precision = 10, scale = 6)
    private BigDecimal taxaMensal;

    @Column(name = "cet_anual_informado", precision = 10, scale = 6)
    private BigDecimal cetAnualInformado;

    @Column(name = "valor_parcela", precision = 12, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "apenas_estimativa", nullable = false)
    private Boolean apenasEstimativa = Boolean.TRUE;

    @Column(name = "situacao_externa", nullable = false, length = 40)
    private String situacaoExterna = "SIMULADO";

    @Column(name = "valido_ate", columnDefinition = "datetime")
    private OffsetDateTime validoAte;
}
