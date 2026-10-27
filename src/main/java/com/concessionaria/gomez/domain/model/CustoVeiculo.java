package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "custo_veiculo")
public class CustoVeiculo {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 45)
    private NaturezaItemFinanceiro natureza;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_calculo", nullable = false, length = 35)
    private BaseCalculoItemFinanceiro baseCalculo = BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS;

    @Column(precision = 12, scale = 6)
    private BigDecimal valor;

    @Column(nullable = false)
    private Boolean confirmado = Boolean.FALSE;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(length = 100)
    private String responsavel;

    @CreationTimestamp
    @Column(name = "data_registro", nullable = false, columnDefinition = "datetime", updatable = false)
    private OffsetDateTime dataRegistro;

    public boolean isPendenteConfirmacao() {
        return !Boolean.TRUE.equals(confirmado) || valor == null;
    }
}
