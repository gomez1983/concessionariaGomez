package com.concessionaria.gomez.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Operador do painel (vendedor, gerente ou administrador).
 * O e-mail é o identificador gravado nos campos de auditoria das propostas e vendas.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "usuario")
public class Usuario {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @ToString.Exclude
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilOperadorComercial perfil;

    @Column(nullable = false)
    private Boolean ativo = Boolean.TRUE;

    @Column(name = "tentativas_falhas", nullable = false)
    private Integer tentativasFalhas = 0;

    @Column(name = "bloqueado_ate", columnDefinition = "datetime")
    private OffsetDateTime bloqueadoAte;

    @Column(name = "ultimo_login", columnDefinition = "datetime")
    private OffsetDateTime ultimoLogin;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, columnDefinition = "datetime", updatable = false)
    private OffsetDateTime dataCadastro;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false, columnDefinition = "datetime")
    private OffsetDateTime dataAtualizacao;

    public boolean estaBloqueado(OffsetDateTime agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    /** Conta uma senha errada; ao atingir o limite, bloqueia o acesso pelo período informado. */
    public void registrarFalhaDeLogin(int limiteTentativas, OffsetDateTime bloquearAte) {
        tentativasFalhas = (tentativasFalhas == null ? 0 : tentativasFalhas) + 1;
        if (tentativasFalhas >= limiteTentativas) {
            bloqueadoAte = bloquearAte;
            tentativasFalhas = 0;
        }
    }

    public void registrarLoginBemSucedido(OffsetDateTime agora) {
        desbloquear();
        ultimoLogin = agora;
    }

    /** Zera o contador de senhas erradas e encerra um bloqueio em andamento. */
    public void desbloquear() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    /** Troca o hash da senha; o bloqueio por tentativas deixa de valer, pois a senha é outra. */
    public void redefinirSenha(String novoHash) {
        senhaHash = novoHash;
        desbloquear();
    }

    public void ativar() {
        ativo = Boolean.TRUE;
        desbloquear();
    }

    public void inativar() {
        ativo = Boolean.FALSE;
    }
}
