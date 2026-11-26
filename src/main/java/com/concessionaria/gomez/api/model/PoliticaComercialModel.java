package com.concessionaria.gomez.api.model;

import com.concessionaria.gomez.domain.model.ContextoPoliticaComercial;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Representação de saída da política comercial. Referencia o veículo apenas pelo ID,
 * para não serializar a entidade {@code Carro} (com o custo de aquisição) nem proxies do Hibernate.
 */
@Getter
@Setter
public class PoliticaComercialModel {

    private Long id;
    private ContextoPoliticaComercial contexto;
    private Long carroId;
    private Integer versao;
    private BigDecimal margemDesejada;
    private BigDecimal margemAutonomia;
    private BigDecimal margemPisoAbsoluto;
    private BigDecimal descontoMaximoAutonomiaReais;
    private BigDecimal descontoMaximoAutonomiaPercentual;
    private String responsavelCadastro;
    private OffsetDateTime vigenteAPartirDe;
    private OffsetDateTime vigenteAte;
    private Boolean ativa;
    private OffsetDateTime dataCadastro;
}
