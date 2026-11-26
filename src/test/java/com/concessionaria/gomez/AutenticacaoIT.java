package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.exception.AutenticacaoException;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.SessaoUsuario;
import com.concessionaria.gomez.domain.model.SessaoUsuario.MotivoRevogacao;
import com.concessionaria.gomez.domain.model.Usuario;
import com.concessionaria.gomez.domain.repository.SessaoUsuarioRepository;
import com.concessionaria.gomez.domain.repository.UsuarioRepository;
import com.concessionaria.gomez.domain.service.AutenticacaoService;
import com.concessionaria.gomez.domain.service.PoliticaSenha;
import com.concessionaria.gomez.util.DatabaseCleaner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, bloqueio por tentativas, rotação do refresh token, detecção de reuso e logout,
 * contra o banco de teste e a cadeia de filtros real do Spring Security.
 */
@SpringBootTest
@TestPropertySource("/application-test.properties")
class AutenticacaoIT {

    private static final String COOKIE = "gomez_refresh";
    private static final String ANTI_CSRF = "X-Requested-With";
    private static final String ANTI_CSRF_VALOR = "gomez-spa";
    private static final Pattern VALOR_COOKIE = Pattern.compile(COOKIE + "=([^;]*)");

    private static final String EMAIL = "vendedora@teste.gomez";
    private static final String SENHA = "senha-de-teste-123";

    @Autowired WebApplicationContext contexto;
    @Autowired DatabaseCleaner cleaner;
    @Autowired UsuarioRepository usuarios;
    @Autowired SessaoUsuarioRepository sessoes;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    MockMvc mvc;
    Usuario usuario;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        cleaner.clearTables();

        usuario = new Usuario();
        usuario.setNome("Vendedora de Teste");
        usuario.setEmail(EMAIL);
        usuario.setSenhaHash(passwordEncoder.encode(SENHA));
        usuario.setPerfil(PerfilOperadorComercial.VENDEDOR);
        usuario = usuarios.saveAndFlush(usuario);
    }

    // ---------------------------------------------------------------- login

    @Test
    @DisplayName("Login válido devolve access token no corpo e refresh token só em cookie HttpOnly/SameSite=Strict")
    void loginValido() throws Exception {
        MvcResult resultado = login(EMAIL, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(EMAIL))
                .andExpect(jsonPath("$.usuario.perfil").value("VENDEDOR"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains(COOKIE + "=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/auth")
                .doesNotContain("Secure"); // cookie-secure=false no perfil de teste (http)
        assertThat(maxAge(setCookie)).isBetween(35_990L, 36_000L); // 10 h

        // O refresh token não aparece no corpo e só o hash fica no banco
        String refresh = valorDoCookie(resultado);
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain(refresh);
        assertThat(sessoes.findAll()).singleElement()
                .satisfies(s -> assertThat(s.getTokenHash()).hasSize(64).isNotEqualTo(refresh));

        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(resultado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.nome").value("Vendedora de Teste"))
                .andExpect(jsonPath("$.perfil").value("VENDEDOR"));

        Usuario atualizado = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(atualizado.getUltimoLogin()).isNotNull();
    }

    @Test
    @DisplayName("E-mail inexistente e senha errada recebem 401 com a mesma mensagem genérica e sem cookie")
    void falhasDeLoginSaoIndistinguiveis() throws Exception {
        login("ninguem@teste.gomez", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_CREDENCIAIS_INVALIDAS))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        login(EMAIL, "senha-errada-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_CREDENCIAIS_INVALIDAS))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertThat(sessoes.count()).isZero();
    }

    @Test
    @DisplayName("Senha acima de 72 bytes (72 caracteres acentuados) é senha errada, não erro interno")
    void senhaAcimaDoLimiteDoBcrypt() throws Exception {
        login(EMAIL, "ç".repeat(72))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_CREDENCIAIS_INVALIDAS));

        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getTentativasFalhas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Login sem e-mail ou senha é rejeitado na validação (400)")
    void loginSemCampos() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Após 5 senhas erradas a conta fica bloqueada, inclusive para a senha certa, até o bloqueio expirar")
    void bloqueioPorTentativas() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(EMAIL, "senha-errada-" + i).andExpect(status().isUnauthorized());
        }

        login(EMAIL, SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_CREDENCIAIS_INVALIDAS));

        Usuario bloqueado = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(bloqueado.getBloqueadoAte()).isAfter(OffsetDateTime.now().plusMinutes(14));

        // Fim do bloqueio: o login volta a funcionar e o contador é zerado
        jdbc.update("update usuario set bloqueado_ate = date_sub(now(), interval 1 minute) where id = ?", usuario.getId());
        login(EMAIL, SENHA).andExpect(status().isOk());

        Usuario liberado = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(liberado.getBloqueadoAte()).isNull();
        assertThat(liberado.getTentativasFalhas()).isZero();
    }

    @Test
    @DisplayName("Usuário inativo não entra e recebe a mesma mensagem genérica")
    void usuarioInativoNaoEntra() throws Exception {
        jdbc.update("update usuario set ativo = false where id = ?", usuario.getId());

        login(EMAIL, SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_CREDENCIAIS_INVALIDAS));
    }

    // ---------------------------------------------------------------- refresh

    @Test
    @DisplayName("Refresh rotaciona o cookie: novo token de sessão, a sessão anterior fica revogada como ROTACIONADA")
    void refreshRotaciona() throws Exception {
        String primeiro = valorDoCookie(login(EMAIL, SENHA).andReturn());

        MvcResult renovado = refresh(primeiro)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String segundo = valorDoCookie(renovado);
        assertThat(segundo).isNotBlank().isNotEqualTo(primeiro);

        // A sessão nova herda o limite absoluto de 10 h: o Max-Age não é reiniciado
        assertThat(maxAge(renovado.getResponse().getHeader(HttpHeaders.SET_COOKIE))).isLessThanOrEqualTo(36_000L);

        Map<MotivoRevogacao, Long> porMotivo = motivos();
        assertThat(porMotivo).containsEntry(MotivoRevogacao.ROTACIONADA, 1L).containsEntry(null, 1L);

        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(renovado)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Refresh sem X-Requested-With é recusado (403) e não rotaciona a sessão")
    void refreshExigeCabecalhoAntiCsrf() throws Exception {
        String refresh = valorDoCookie(login(EMAIL, SENHA).andReturn());

        mvc.perform(post("/auth/refresh").cookie(new Cookie(COOKIE, refresh)))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertThat(motivos()).containsOnlyKeys((MotivoRevogacao) null);
    }

    @Test
    @DisplayName("Refresh sem cookie ou com cookie desconhecido recebe 401 de sessão expirada")
    void refreshSemSessao() throws Exception {
        mvc.perform(post("/auth/refresh").header(ANTI_CSRF, ANTI_CSRF_VALOR))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_SESSAO_EXPIRADA));

        refresh("token-que-nunca-existiu")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_SESSAO_EXPIRADA));
    }

    @Test
    @DisplayName("Reapresentar o token antigo dentro da tolerância (corrida legítima) devolve só um access token, sem cookie")
    void reusoDentroDaTolerancia() throws Exception {
        String primeiro = valorDoCookie(login(EMAIL, SENHA).andReturn());
        String segundo = valorDoCookie(refresh(primeiro).andExpect(status().isOk()).andReturn());

        refresh(primeiro)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        // Nada foi revogado por reuso: a sessão substituta continua válida
        assertThat(motivos()).doesNotContainKey(MotivoRevogacao.REUSO_DETECTADO);
        refresh(segundo).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reapresentar o token antigo fora da tolerância revoga todas as sessões do usuário (indício de roubo)")
    void reusoForaDaToleranciaRevogaFamilia() throws Exception {
        String primeiro = valorDoCookie(login(EMAIL, SENHA).andReturn());
        String segundo = valorDoCookie(refresh(primeiro).andExpect(status().isOk()).andReturn());

        // Outra sessão do mesmo usuário (outro computador) também cai
        String outroDispositivo = valorDoCookie(login(EMAIL, SENHA).andReturn());

        jdbc.update("update sessao_usuario set revogada_em = date_sub(revogada_em, interval 1 minute) "
                + "where motivo_revogacao = 'ROTACIONADA'");

        refresh(primeiro)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_SESSAO_EXPIRADA));

        assertThat(motivos())
                .containsEntry(MotivoRevogacao.REUSO_DETECTADO, 2L)
                .doesNotContainKey(null);
        refresh(segundo).andExpect(status().isUnauthorized());
        refresh(outroDispositivo).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sessão além do limite absoluto não renova")
    void sessaoExpiradaNaoRenova() throws Exception {
        String refresh = valorDoCookie(login(EMAIL, SENHA).andReturn());
        jdbc.update("update sessao_usuario set expira_em = date_sub(now(), interval 1 minute)");

        refresh(refresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoException.MSG_SESSAO_EXPIRADA));
    }

    @Test
    @DisplayName("Usuário desativado com sessão aberta não renova e tem as sessões revogadas")
    void usuarioDesativadoNaoRenova() throws Exception {
        String refresh = valorDoCookie(login(EMAIL, SENHA).andReturn());
        jdbc.update("update usuario set ativo = false where id = ?", usuario.getId());

        refresh(refresh).andExpect(status().isUnauthorized());
        assertThat(motivos()).containsEntry(MotivoRevogacao.USUARIO_INATIVO, 1L);
    }

    // ---------------------------------------------------------------- troca de senha

    private static final String NOVA_SENHA = "nova-senha-de-teste-456";

    @Test
    @DisplayName("Trocar a senha mantém a sessão deste navegador, encerra as outras e invalida a senha antiga")
    void trocaDeSenha() throws Exception {
        MvcResult nesteNavegador = login(EMAIL, SENHA).andReturn();
        String cookieAtual = valorDoCookie(nesteNavegador);
        String outroNavegador = valorDoCookie(login(EMAIL, SENHA).andReturn());

        trocarSenha(accessToken(nesteNavegador), cookieAtual, SENHA, NOVA_SENHA)
                .andExpect(status().isNoContent());

        assertThat(motivos()).containsEntry(MotivoRevogacao.SENHA_ALTERADA, 1L).containsEntry(null, 1L);
        refresh(outroNavegador).andExpect(status().isUnauthorized());
        refresh(cookieAtual).andExpect(status().isOk());

        login(EMAIL, SENHA).andExpect(status().isUnauthorized());
        login(EMAIL, NOVA_SENHA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sem o cookie (ex.: Postman), a troca de senha encerra todas as sessões")
    void trocaDeSenhaSemCookieEncerraTodas() throws Exception {
        String token = accessToken(login(EMAIL, SENHA).andReturn());

        trocarSenha(token, null, SENHA, NOVA_SENHA).andExpect(status().isNoContent());

        assertThat(motivos()).containsOnlyKeys(MotivoRevogacao.SENHA_ALTERADA);
    }

    @Test
    @DisplayName("Senha atual errada responde 400 (não 401, que derrubaria a sessão no painel) e conta para o bloqueio")
    void trocaDeSenhaComSenhaAtualErrada() throws Exception {
        String token = accessToken(login(EMAIL, SENHA).andReturn());

        for (int i = 0; i < 5; i++) {
            trocarSenha(token, null, "senha-atual-errada", NOVA_SENHA)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.userMessage").value(AutenticacaoService.MSG_SENHA_ATUAL_INCORRETA));
        }
        // Bloqueado: nem a senha atual certa troca a senha durante o bloqueio
        trocarSenha(token, null, SENHA, NOVA_SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userMessage").value(AutenticacaoService.MSG_TROCA_BLOQUEADA));

        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getBloqueadoAte()).isNotNull();
        login(EMAIL, SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Nova senha fora da política (curta, acima de 72 bytes ou igual à atual) é recusada com 400")
    void novaSenhaForaDaPolitica() throws Exception {
        String token = accessToken(login(EMAIL, SENHA).andReturn());

        trocarSenha(token, null, SENHA, "curta").andExpect(status().isBadRequest());
        trocarSenha(token, null, SENHA, "ç".repeat(40))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userMessage").value(PoliticaSenha.MSG_SENHA_INVALIDA));
        trocarSenha(token, null, SENHA, SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userMessage").value("A nova senha deve ser diferente da atual."));

        login(EMAIL, SENHA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Trocar senha exige token (401 sem Authorization)")
    void trocaDeSenhaExigeToken() throws Exception {
        mvc.perform(put("/auth/senha").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("senhaAtual", SENHA, "novaSenha", NOVA_SENHA))))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- logout e token

    @Test
    @DisplayName("Logout revoga a sessão e apaga o cookie; o refresh token deixa de valer")
    void logout() throws Exception {
        String refresh = valorDoCookie(login(EMAIL, SENHA).andReturn());

        MvcResult saida = mvc.perform(post("/auth/logout")
                        .cookie(new Cookie(COOKIE, refresh))
                        .header(ANTI_CSRF, ANTI_CSRF_VALOR))
                .andExpect(status().isNoContent())
                .andReturn();

        String setCookie = saida.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains(COOKIE + "=;").contains("Path=/auth");
        assertThat(maxAge(setCookie)).isZero();
        assertThat(motivos()).containsEntry(MotivoRevogacao.LOGOUT, 1L);

        refresh(refresh).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout sem X-Requested-With é recusado (403) e a sessão continua ativa")
    void logoutExigeCabecalhoAntiCsrf() throws Exception {
        String refresh = valorDoCookie(login(EMAIL, SENHA).andReturn());

        mvc.perform(post("/auth/logout").cookie(new Cookie(COOKIE, refresh)))
                .andExpect(status().isForbidden());

        assertThat(motivos()).containsOnlyKeys((MotivoRevogacao) null);
    }

    @Test
    @DisplayName("Access token adulterado é recusado com 401")
    void tokenAdulterado() throws Exception {
        String token = accessToken(login(EMAIL, SENHA).andReturn());
        String[] partes = token.split("\\.");
        // Troca o perfil no payload mantendo a assinatura original
        String payload = new String(java.util.Base64.getUrlDecoder().decode(partes[1]))
                .replace("\"VENDEDOR\"", "\"ADMIN\"");
        String adulterado = partes[0] + "."
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes()) + "."
                + partes[2];

        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- utilitários

    private org.springframework.test.web.servlet.ResultActions login(String email, String senha) throws Exception {
        String corpo = mapper.writeValueAsString(Map.of("email", email, "senha", senha));
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh")
                .cookie(new Cookie(COOKIE, refreshToken))
                .header(ANTI_CSRF, ANTI_CSRF_VALOR));
    }

    private org.springframework.test.web.servlet.ResultActions trocarSenha(
            String accessToken, String cookie, String senhaAtual, String novaSenha) throws Exception {
        var requisicao = put("/auth/senha")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("senhaAtual", senhaAtual, "novaSenha", novaSenha)));
        if (cookie != null) requisicao.cookie(new Cookie(COOKIE, cookie));
        return mvc.perform(requisicao);
    }

    private static String valorDoCookie(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie").isNotNull();
        Matcher m = VALOR_COOKIE.matcher(setCookie);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static long maxAge(String setCookie) {
        Matcher m = Pattern.compile("Max-Age=(\\d+)").matcher(setCookie);
        assertThat(m.find()).as("Max-Age em " + setCookie).isTrue();
        return Long.parseLong(m.group(1));
    }

    private String accessToken(MvcResult resultado) throws Exception {
        JsonNode corpo = mapper.readTree(resultado.getResponse().getContentAsString());
        return corpo.get("accessToken").asText();
    }

    /** Contagem de sessões por motivo de revogação; a chave {@code null} conta as sessões ativas. */
    private Map<MotivoRevogacao, Long> motivos() {
        List<SessaoUsuario> todas = sessoes.findAll();
        Map<MotivoRevogacao, Long> contagem = new java.util.HashMap<>();
        todas.forEach(s -> contagem.merge(s.getMotivoRevogacao(), 1L, Long::sum));
        return contagem;
    }
}
