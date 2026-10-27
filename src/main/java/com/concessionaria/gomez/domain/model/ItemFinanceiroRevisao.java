package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "item_financeiro_revisao")
public class ItemFinanceiroRevisao {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "revisao_id", nullable = false)
    private RevisaoProposta revisao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 45)
    private NaturezaItemFinanceiro natureza;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_calculo", nullable = false, length = 35)
    private BaseCalculoItemFinanceiro baseCalculo;

    @Column(precision = 12, scale = 6)
    private BigDecimal valor;

    @Column(nullable = false)
    private Boolean confirmado = Boolean.FALSE;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(length = 80)
    private String origem;

    public boolean isPendenteConfirmacao() {
        return !Boolean.TRUE.equals(confirmado) || valor == null;
    }
}
