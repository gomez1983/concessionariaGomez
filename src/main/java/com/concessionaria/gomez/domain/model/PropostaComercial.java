package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "proposta_comercial")
public class PropostaComercial {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "vendedor_identificador", nullable = false, length = 100)
    private String vendedorIdentificador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPropostaComercial status = StatusPropostaComercial.RASCUNHO;

    @Column(name = "revisao_atual_sequencia")
    private Integer revisaoAtualSequencia = 1;

    @Column(name = "valida_ate", columnDefinition = "datetime")
    private OffsetDateTime validaAte;

    @Version
    @Column(name = "versao_concorrencia", nullable = false)
    private Long versaoConcorrencia = 0L;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, columnDefinition = "datetime", updatable = false)
    private OffsetDateTime dataCriacao;

    @OneToMany(mappedBy = "proposta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RevisaoProposta> revisoes = new ArrayList<>();
}
