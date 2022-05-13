package com.concessionaria.gomez.domain.model;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
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
    private String modelo;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false)
    private double compra;

    //Atributo adicionado. Corrigir saída no Postman
    @CreationTimestamp
    @Column(name = "datacompra", nullable = false, columnDefinition = "datetime")
    private OffsetDateTime dataCompra;

}
