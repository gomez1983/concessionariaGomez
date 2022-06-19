package com.concessionaria.gomez.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.OffsetDateTime;


@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Carro {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    @NotBlank //Adicionada anotação para que o teste de integração de cadastro Null funcione
    private String modelo;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Double compra;

    private Boolean ativo = Boolean.TRUE;

    //Atributo adicionado. Corrigir saída no Postman
    @CreationTimestamp
    @Column(name = "datacompra", nullable = false, columnDefinition = "datetime")
    private OffsetDateTime dataCompra;

    @PreUpdate
    private void setData() {
        this.dataCompra = OffsetDateTime.now();
    }

    public void ativar() {
        setAtivo(true);
    }

    public void inativar() {
        setAtivo(false);
    }
}