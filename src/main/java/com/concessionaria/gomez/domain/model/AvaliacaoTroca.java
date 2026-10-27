package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "avaliacao_troca")
public class AvaliacaoTroca {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "revisao_id", nullable = false, unique = true)
    private RevisaoProposta revisao;

    @Column(name = "identificacao_veiculo_usado", nullable = false, length = 150)
    private String identificacaoVeiculoUsado;

    @Column(name = "valor_avaliacao_tecnica", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAvaliacaoTecnica;

    @Column(name = "valor_comercial_aceito", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorComercialAceito;

    @Column(name = "saldo_quitacao_verificado", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoQuitacaoVerificado = BigDecimal.ZERO;

    @Column(name = "credito_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditoLiquido = BigDecimal.ZERO;

    @Column(name = "possui_pendencia_documental", nullable = false)
    private Boolean possuiPendenciaDocumental = Boolean.FALSE;

    @Column(name = "descricao_pendencias", length = 255)
    private String descricaoPendencias;

    @Column(name = "valida_ate", columnDefinition = "datetime")
    private OffsetDateTime validaAte;

    public BigDecimal calcularCreditoLiquido() {
        if (valorComercialAceito == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal quitacao = saldoQuitacaoVerificado != null ? saldoQuitacaoVerificado : BigDecimal.ZERO;
        return valorComercialAceito.subtract(quitacao).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean possuiSaldoDevedorSuperiorAvaliacao() {
        return calcularCreditoLiquido().compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal calcularSuperavaliacaoSuportadaPelaLoja() {
        if (valorComercialAceito == null || valorAvaliacaoTecnica == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal diferenca = valorComercialAceito.subtract(valorAvaliacaoTecnica).setScale(2, RoundingMode.HALF_UP);
        return diferenca.compareTo(BigDecimal.ZERO) > 0 ? diferenca : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
