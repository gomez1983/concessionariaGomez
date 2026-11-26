package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Sessão de renovação (refresh token). Somente o SHA-256 do token é gravado.
 * A cada renovação a sessão é substituída por outra com o mesmo limite absoluto ({@code expiraEm}).
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "sessao_usuario")
public class SessaoUsuario {

    public enum MotivoRevogacao { ROTACIONADA, LOGOUT, REUSO_DETECTADO, USUARIO_INATIVO, SENHA_ALTERADA }

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ToString.Exclude
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "criada_em", nullable = false, columnDefinition = "datetime")
    private OffsetDateTime criadaEm;

    @Column(name = "expira_em", nullable = false, columnDefinition = "datetime")
    private OffsetDateTime expiraEm;

    @Column(name = "revogada_em", columnDefinition = "datetime")
    private OffsetDateTime revogadaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_revogacao", length = 20)
    private MotivoRevogacao motivoRevogacao;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substituida_por_id")
    private SessaoUsuario substituidaPor;

    public boolean estaRevogada() {
        return revogadaEm != null;
    }

    public boolean estaExpirada(OffsetDateTime agora) {
        return !expiraEm.isAfter(agora);
    }

    public boolean estaAtiva(OffsetDateTime agora) {
        return !estaRevogada() && !estaExpirada(agora);
    }

    public void revogar(MotivoRevogacao motivo, OffsetDateTime agora) {
        if (!estaRevogada()) {
            revogadaEm = agora;
            motivoRevogacao = motivo;
        }
    }

    /**
     * Uma sessão rotacionada há poucos segundos ainda é reapresentada em corridas legítimas
     * (duas abas, React StrictMode, rede instável). Isso não deve ser tratado como roubo de token.
     */
    public boolean foiRotacionadaHaMenosDe(Duration tolerancia, OffsetDateTime agora) {
        return motivoRevogacao == MotivoRevogacao.ROTACIONADA
                && revogadaEm != null
                && !revogadaEm.plus(tolerancia).isBefore(agora);
    }
}
