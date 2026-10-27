package com.concessionaria.gomez.domain.model;

import com.concessionaria.gomez.domain.exception.NegocioException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "politica_comercial")
public class PoliticaComercial {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContextoPoliticaComercial contexto = ContextoPoliticaComercial.LOJA_PADRAO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carro_id")
    private Carro carro;

    @Column(nullable = false)
    private Integer versao = 1;

    @Column(name = "margem_desejada", nullable = false, precision = 10, scale = 6)
    private BigDecimal margemDesejada;

    @Column(name = "margem_autonomia", nullable = false, precision = 10, scale = 6)
    private BigDecimal margemAutonomia;

    @Column(name = "margem_piso_absoluto", nullable = false, precision = 10, scale = 6)
    private BigDecimal margemPisoAbsoluto;

    @Column(name = "desconto_maximo_autonomia_reais", precision = 12, scale = 2)
    private BigDecimal descontoMaximoAutonomiaReais;

    @Column(name = "desconto_maximo_autonomia_percentual", precision = 10, scale = 6)
    private BigDecimal descontoMaximoAutonomiaPercentual;

    @Column(name = "responsavel_cadastro", nullable = false, length = 100)
    private String responsavelCadastro;

    @Column(name = "vigente_a_partir_de", nullable = false, columnDefinition = "datetime")
    private OffsetDateTime vigenteAPartirDe;

    @Column(name = "vigente_ate", columnDefinition = "datetime")
    private OffsetDateTime vigenteAte;

    @Column(nullable = false)
    private Boolean ativa = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, columnDefinition = "datetime", updatable = false)
    private OffsetDateTime dataCadastro;

    public void validarInvariantes() {
        if (contexto == null) {
            throw new NegocioException("O contexto da política comercial é obrigatório.");
        }
        if (contexto == ContextoPoliticaComercial.EXCECAO_VEICULO && carro == null) {
            throw new NegocioException("Política de exceção por veículo exige o veículo vinculado.");
        }
        if (contexto == ContextoPoliticaComercial.LOJA_PADRAO && carro != null) {
            throw new NegocioException("Política padrão da loja não deve estar vinculada a um veículo específico.");
        }
        if (margemDesejada == null || margemAutonomia == null || margemPisoAbsoluto == null) {
            throw new NegocioException("As margens desejada, de autonomia e piso absoluto são obrigatórias.");
        }
        if (margemPisoAbsoluto.compareTo(BigDecimal.ZERO) < 0
                || margemDesejada.compareTo(BigDecimal.ONE) >= 0) {
            throw new NegocioException("As margens de contribuição devem estar no intervalo [0, 1).");
        }
        if (margemDesejada.compareTo(margemAutonomia) < 0
                || margemAutonomia.compareTo(margemPisoAbsoluto) < 0) {
            throw new NegocioException("Invariante de política violada: exige-se margemDesejada >= margemAutonomia >= margemPisoAbsoluto.");
        }
        if (descontoMaximoAutonomiaReais != null && descontoMaximoAutonomiaReais.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("O limite de desconto em reais não pode ser negativo.");
        }
        if (descontoMaximoAutonomiaPercentual != null
                && (descontoMaximoAutonomiaPercentual.compareTo(BigDecimal.ZERO) < 0
                || descontoMaximoAutonomiaPercentual.compareTo(BigDecimal.ONE) >= 0)) {
            throw new NegocioException("O limite de desconto percentual deve estar no intervalo [0, 1).");
        }
        if (vigenteAPartirDe != null && vigenteAte != null && vigenteAte.isBefore(vigenteAPartirDe)) {
            throw new NegocioException("A data final de vigência não pode ser anterior ao início da vigência.");
        }
    }

    public boolean estaVigenteEm(OffsetDateTime instante) {
        if (!Boolean.TRUE.equals(ativa) || instante == null || vigenteAPartirDe == null) {
            return false;
        }
        boolean aposInicio = !instante.isBefore(vigenteAPartirDe);
        boolean antesFim = vigenteAte == null || !instante.isAfter(vigenteAte);
        return aposInicio && antesFim;
    }
}
