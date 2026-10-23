package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "foto_galeria_carro")
public class FotoGaleriaCarro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @Column(name = "nome_arquivo", nullable = false, length = 150)
    private String nomeArquivo;
    @Column(nullable = false, length = 150)
    private String descricao;
    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;
    @Column(nullable = false)
    private Long tamanho;
}
