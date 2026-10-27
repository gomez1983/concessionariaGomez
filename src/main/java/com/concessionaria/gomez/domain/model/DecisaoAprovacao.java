package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "decisao_aprovacao")
public class DecisaoAprovacao {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "revisao_id", nullable = false)
    private RevisaoProposta revisao;

    @Column(name = "decisor_autenticado", nullable = false, length = 100)
    private String decisorAutenticado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ResultadoDecisaoAprovacao resultado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AlcadaAnaliseFinanceira alcada;

    @Column(length = 255)
    private String motivo;

    @CreationTimestamp
    @Column(name = "data_decisao", nullable = false, columnDefinition = "datetime", updatable = false)
    private OffsetDateTime dataDecisao;
}
