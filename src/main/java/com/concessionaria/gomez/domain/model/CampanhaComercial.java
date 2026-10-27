package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "campanha_comercial")
public class CampanhaComercial {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 40)
    private String origem;

    @Column(name = "valor_beneficio", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBeneficio;

    @Column(name = "reembolsavel_montadora", nullable = false)
    private Boolean reembolsavelMontadora = Boolean.FALSE;

    @Column(name = "ja_refletida_no_preco_ou_custo", nullable = false)
    private Boolean jaRefletidaNoPrecoOuCusto = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean cumulativa = Boolean.FALSE;

    @Column(name = "vigente_a_partir_de", nullable = false, columnDefinition = "datetime")
    private OffsetDateTime vigenteAPartirDe;

    @Column(name = "vigente_ate", columnDefinition = "datetime")
    private OffsetDateTime vigenteAte;

    @Column(nullable = false)
    private Boolean ativa = Boolean.TRUE;

    @Column(name = "evidencia_condicoes", length = 255)
    private String evidenciaCondicoes;

    public boolean isElegivelComoReceitaAdicional() {
        return Boolean.TRUE.equals(ativa)
                && Boolean.TRUE.equals(reembolsavelMontadora)
                && !Boolean.TRUE.equals(jaRefletidaNoPrecoOuCusto)
                && valorBeneficio != null
                && valorBeneficio.compareTo(BigDecimal.ZERO) > 0;
    }
}
