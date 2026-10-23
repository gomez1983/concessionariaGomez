package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "foto_carro")
public class FotoCarro {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "carro_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private Carro carro;

    @Column(name = "nome_arquivo", nullable = false, length = 150)
    private String nomeArquivo;

    @Column(length = 150)
    private String descricao;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(nullable = false)
    private Long tamanho;

    public Long getCarroId() {
        if (getCarro() != null) {
            return getCarro().getId();
        }
        return this.id;
    }
}
