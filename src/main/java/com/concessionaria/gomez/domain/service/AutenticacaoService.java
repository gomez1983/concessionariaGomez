package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.core.security.AuthProperties;
import com.concessionaria.gomez.domain.exception.AutenticacaoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.SessaoUsuario;
import com.concessionaria.gomez.domain.model.SessaoUsuario.MotivoRevogacao;
import com.concessionaria.gomez.domain.model.Usuario;
import com.concessionaria.gomez.domain.repository.SessaoUsuarioRepository;
import com.concessionaria.gomez.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Login com bloqueio por tentativas, sessões de renovação rotativas e logout.
 * Toda falha devolve a mesma mensagem, para não revelar se o e-mail existe ou se a conta está bloqueada.
 */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private static final SecureRandom GERADOR = new SecureRandom();
    private static final int TAMANHO_REFRESH_BYTES = 32;

    private final UsuarioRepository usuarioRepository;
    private final SessaoUsuarioRepository sessaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    public static final String MSG_SENHA_ATUAL_INCORRETA = "A senha atual não confere.";
    public static final String MSG_TROCA_BLOQUEADA =
            "Muitas tentativas com a senha atual incorreta. Aguarde alguns minutos e tente novamente.";

    private volatile String hashFicticio;

    /**
     * @param refreshToken valor puro do novo refresh token, ou {@code null} quando a sessão não foi rotacionada
     *                     (reapresentação dentro da tolerância: o navegador já recebeu o cookie da sessão substituta).
     */
    public record ResultadoAutenticacao(Usuario usuario, String refreshToken, OffsetDateTime sessaoExpiraEm) {
    }

    @Transactional(noRollbackFor = AutenticacaoException.class)
    public ResultadoAutenticacao autenticar(String email, String senha) {
        OffsetDateTime agora = OffsetDateTime.now();
        // O BCrypt recusa (exceção) senhas acima de 72 bytes; nenhuma senha cadastrada passa desse limite,
        // então uma senha maior é simplesmente errada. Compara uma vazia para manter o mesmo tempo de resposta.
        boolean excedeLimite = senha != null && PoliticaSenha.excedeLimiteBcrypt(senha);
        String senhaInformada = senha == null || excedeLimite ? "" : senha;
        Optional<Usuario> encontrado = email == null || email.isBlank()
                ? Optional.empty()
                : usuarioRepository.findByEmailIgnoreCase(email.trim());

        if (encontrado.isEmpty()) {
            // Verifica um hash qualquer para o tempo de resposta não revelar quais e-mails existem
            passwordEncoder.matches(senhaInformada, hashFicticio());
            throw AutenticacaoException.credenciaisInvalidas();
        }

        Usuario usuario = encontrado.get();
        boolean senhaConfere = passwordEncoder.matches(senhaInformada, usuario.getSenhaHash()) && !excedeLimite;

        if (usuario.estaBloqueado(agora) || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw AutenticacaoException.credenciaisInvalidas();
        }
        if (!senhaConfere) {
            usuario.registrarFalhaDeLogin(properties.maxTentativas(), agora.plus(properties.bloqueio()));
            usuarioRepository.save(usuario);
            throw AutenticacaoException.credenciaisInvalidas();
        }

        usuario.registrarLoginBemSucedido(agora);
        usuarioRepository.save(usuario);

        String refreshToken = gerarRefreshToken();
        SessaoUsuario sessao = criarSessao(usuario, refreshToken, agora, agora.plus(properties.sessionTtl()));
        return new ResultadoAutenticacao(usuario, refreshToken, sessao.getExpiraEm());
    }

    @Transactional(noRollbackFor = AutenticacaoException.class)
    public ResultadoAutenticacao renovar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AutenticacaoException.sessaoExpirada();
        }
        OffsetDateTime agora = OffsetDateTime.now();
        SessaoUsuario sessao = sessaoRepository.findByTokenHashParaAtualizacao(hash(refreshToken))
                .orElseThrow(AutenticacaoException::sessaoExpirada);
        Usuario usuario = sessao.getUsuario();

        if (sessao.estaRevogada()) {
            SessaoUsuario substituta = sessao.getSubstituidaPor();
            if (sessao.foiRotacionadaHaMenosDe(properties.refreshReuseGrace(), agora)
                    && substituta != null && substituta.estaAtiva(agora)
                    && Boolean.TRUE.equals(usuario.getAtivo())) {
                // Corrida legítima (duas abas, StrictMode, rede instável): emite só um novo access token
                return new ResultadoAutenticacao(usuario, null, substituta.getExpiraEm());
            }
            if (sessao.getMotivoRevogacao() == MotivoRevogacao.ROTACIONADA) {
                // Token já rotacionado reapresentado fora da tolerância: indício de roubo
                revogarSessoes(usuario, MotivoRevogacao.REUSO_DETECTADO, agora, null);
            }
            throw AutenticacaoException.sessaoExpirada();
        }
        if (sessao.estaExpirada(agora)) {
            throw AutenticacaoException.sessaoExpirada();
        }
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            revogarSessoes(usuario, MotivoRevogacao.USUARIO_INATIVO, agora, null);
            throw AutenticacaoException.sessaoExpirada();
        }

        // Rotação: a nova sessão herda o limite absoluto da anterior
        String novoRefresh = gerarRefreshToken();
        SessaoUsuario nova = criarSessao(usuario, novoRefresh, agora, sessao.getExpiraEm());
        sessao.revogar(MotivoRevogacao.ROTACIONADA, agora);
        sessao.setSubstituidaPor(nova);
        sessaoRepository.save(sessao);

        return new ResultadoAutenticacao(usuario, novoRefresh, nova.getExpiraEm());
    }

    @Transactional
    public void encerrar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        sessaoRepository.findByTokenHashParaAtualizacao(hash(refreshToken)).ifPresent(sessao -> {
            sessao.revogar(MotivoRevogacao.LOGOUT, OffsetDateTime.now());
            sessaoRepository.save(sessao);
        });
    }

    /**
     * Troca a própria senha. Exige a senha atual (errar conta para o bloqueio, como no login) e encerra as
     * outras sessões do usuário; a sessão do navegador que fez a troca, se o cookie veio junto, continua ativa.
     */
    @Transactional(noRollbackFor = NegocioException.class)
    public void trocarSenha(String email, String senhaAtual, String novaSenha, String refreshTokenAtual) {
        OffsetDateTime agora = OffsetDateTime.now();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .orElseThrow(AutenticacaoException::sessaoExpirada);

        if (usuario.estaBloqueado(agora)) {
            throw new NegocioException(MSG_TROCA_BLOQUEADA);
        }
        boolean excedeLimite = senhaAtual != null && PoliticaSenha.excedeLimiteBcrypt(senhaAtual);
        String atualInformada = senhaAtual == null || excedeLimite ? "" : senhaAtual;
        if (!passwordEncoder.matches(atualInformada, usuario.getSenhaHash()) || excedeLimite) {
            usuario.registrarFalhaDeLogin(properties.maxTentativas(), agora.plus(properties.bloqueio()));
            usuarioRepository.save(usuario);
            throw new NegocioException(MSG_SENHA_ATUAL_INCORRETA);
        }

        PoliticaSenha.validar(novaSenha);
        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new NegocioException("A nova senha deve ser diferente da atual.");
        }

        usuario.redefinirSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        String sessaoMantida = refreshTokenAtual == null || refreshTokenAtual.isBlank() ? null : hash(refreshTokenAtual);
        revogarSessoes(usuario, MotivoRevogacao.SENHA_ALTERADA, agora, sessaoMantida);
    }

    /**
     * Revoga as sessões ativas do usuário. O access token já emitido continua válido até expirar (15 min).
     *
     * @param tokenHashMantido hash da sessão que deve continuar ativa, ou {@code null} para revogar todas
     */
    void revogarSessoes(Usuario usuario, MotivoRevogacao motivo, OffsetDateTime agora, String tokenHashMantido) {
        sessaoRepository.findByUsuarioIdAndRevogadaEmIsNull(usuario.getId()).stream()
                .filter(s -> !s.getTokenHash().equals(tokenHashMantido))
                .forEach(s -> {
                    s.revogar(motivo, agora);
                    sessaoRepository.save(s);
                });
    }

    private SessaoUsuario criarSessao(Usuario usuario, String refreshToken, OffsetDateTime agora, OffsetDateTime expiraEm) {
        SessaoUsuario sessao = new SessaoUsuario();
        sessao.setUsuario(usuario);
        sessao.setTokenHash(hash(refreshToken));
        sessao.setCriadaEm(agora);
        sessao.setExpiraEm(expiraEm);
        return sessaoRepository.save(sessao);
    }

    private String hashFicticio() {
        String hash = hashFicticio;
        if (hash == null) {
            hash = passwordEncoder.encode(UUID.randomUUID().toString());
            hashFicticio = hash;
        }
        return hash;
    }

    private static String gerarRefreshToken() {
        byte[] bytes = new byte[TAMANHO_REFRESH_BYTES];
        GERADOR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }
}
