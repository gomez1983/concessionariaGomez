package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.EntidadeEmUsoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.exception.UsuarioNaoEncontradoException;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.SessaoUsuario.MotivoRevogacao;
import com.concessionaria.gomez.domain.model.Usuario;
import com.concessionaria.gomez.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Administração de usuários do painel (somente ADMIN, garantido pela matriz de segurança).
 * O e-mail é o identificador gravado na auditoria das propostas e vendas, por isso não pode ser alterado.
 * O administrador nunca escolhe a senha de outra pessoa: o sistema gera uma temporária e a mostra uma única vez.
 */
@Service
@RequiredArgsConstructor
public class GestaoUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AutenticacaoService autenticacaoService;

    /** Usuário recém-criado (ou com senha redefinida) e a senha temporária em texto puro, que não é guardada. */
    public record UsuarioComSenhaTemporaria(Usuario usuario, String senhaTemporaria) {
    }

    public Usuario buscarOuFalhar(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
    }

    @Transactional
    public UsuarioComSenhaTemporaria cadastrar(String nome, String email, PerfilOperadorComercial perfil) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new EntidadeEmUsoException(String.format("Já existe um usuário com o e-mail %s.", emailNormalizado));
        }
        String senhaTemporaria = PoliticaSenha.gerarTemporaria();

        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setPerfil(perfil);
        usuario.setSenhaHash(passwordEncoder.encode(senhaTemporaria));
        return new UsuarioComSenhaTemporaria(usuarioRepository.save(usuario), senhaTemporaria);
    }

    /**
     * Altera nome e perfil. O novo perfil vale a partir da próxima renovação do token (em até 15 min),
     * porque o access token é emitido com o perfil lido do banco.
     */
    @Transactional
    public Usuario atualizar(Long usuarioId, String nome, PerfilOperadorComercial perfil, String emailOperador) {
        Usuario usuario = buscarOuFalhar(usuarioId);
        if (ehOProprio(usuario, emailOperador) && usuario.getPerfil() != perfil) {
            // Evita que o último administrador perca o acesso à gestão por engano
            throw new NegocioException("Você não pode alterar o próprio perfil. Peça a outro administrador.");
        }
        usuario.setNome(nome.trim());
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }

    /** Reativa e zera o bloqueio por tentativas: o usuário volta a entrar com a senha que já tinha. */
    @Transactional
    public void ativar(Long usuarioId) {
        Usuario usuario = buscarOuFalhar(usuarioId);
        usuario.ativar();
        usuarioRepository.save(usuario);
    }

    /** Inativa e encerra as sessões: o usuário perde o acesso em até 15 min (validade do access token). */
    @Transactional
    public void inativar(Long usuarioId, String emailOperador) {
        Usuario usuario = buscarOuFalhar(usuarioId);
        if (ehOProprio(usuario, emailOperador)) {
            throw new NegocioException("Você não pode inativar o próprio usuário.");
        }
        usuario.inativar();
        usuarioRepository.save(usuario);
        autenticacaoService.revogarSessoes(usuario, MotivoRevogacao.USUARIO_INATIVO, OffsetDateTime.now(), null);
    }

    /**
     * Gera uma nova senha temporária (para quem esqueceu a senha ou foi bloqueado), zera o bloqueio
     * e encerra todas as sessões do usuário.
     */
    @Transactional
    public UsuarioComSenhaTemporaria redefinirSenhaTemporaria(Long usuarioId, String emailOperador) {
        Usuario usuario = buscarOuFalhar(usuarioId);
        if (ehOProprio(usuario, emailOperador)) {
            throw new NegocioException("Para trocar a própria senha, use \"Trocar senha\" (PUT /auth/senha).");
        }
        String senhaTemporaria = PoliticaSenha.gerarTemporaria();
        usuario.redefinirSenha(passwordEncoder.encode(senhaTemporaria));
        usuarioRepository.save(usuario);
        autenticacaoService.revogarSessoes(usuario, MotivoRevogacao.SENHA_ALTERADA, OffsetDateTime.now(), null);
        return new UsuarioComSenhaTemporaria(usuario, senhaTemporaria);
    }

    private static boolean ehOProprio(Usuario usuario, String emailOperador) {
        return emailOperador != null && usuario.getEmail().equalsIgnoreCase(emailOperador);
    }
}
