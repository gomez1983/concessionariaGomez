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
@Table(name = "venda")
public class Venda {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisao_convertida_id", unique = true)
    private RevisaoProposta revisaoConvertida;

    @Column(name = "origem_legada", nullable = false)
    private Boolean origemLegada = Boolean.TRUE;

    @Column(nullable = false)
    private Boolean cancelada = Boolean.FALSE;

    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;

    @Column(name = "responsavel_cancelamento", length = 100)
    private String responsavelCancelamento;

    @Column(name = "data_cancelamento", columnDefinition = "datetime")
    private OffsetDateTime dataCancelamento;

    @CreationTimestamp
    @Column(name = "data_venda", nullable = false, columnDefinition = "datetime", updatable = false)
    private OffsetDateTime dataVenda;

    @Column(name = "valor_venda", nullable = false)
    private BigDecimal valorVenda;

    @Column(length = 255)
    private String observacoes;
}
