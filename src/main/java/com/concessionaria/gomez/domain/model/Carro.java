package com.concessionaria.gomez.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Carro {

    public static final BigDecimal MARGEM_LUCRO_PADRAO = new BigDecimal("0.10");

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
    private BigDecimal compra;

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

    public BigDecimal calcularPrecoVendaSugerido() {
        return calcularPrecoVendaSugerido(MARGEM_LUCRO_PADRAO);
    }

    public BigDecimal calcularPrecoVendaSugerido(BigDecimal percentualMargem) {
        if (this.compra == null) {
            return null;
        }
        if (percentualMargem == null) {
            percentualMargem = MARGEM_LUCRO_PADRAO;
        }
        BigDecimal multiplicador = BigDecimal.ONE.add(percentualMargem);
        return this.compra.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getPrecoVendaSugerido() {
        return calcularPrecoVendaSugerido();
    }
}
