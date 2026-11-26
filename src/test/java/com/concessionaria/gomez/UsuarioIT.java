package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.SessaoUsuario;
import com.concessionaria.gomez.domain.model.SessaoUsuario.MotivoRevogacao;
import com.concessionaria.gomez.domain.model.Usuario;
import com.concessionaria.gomez.domain.repository.SessaoUsuarioRepository;
import com.concessionaria.gomez.domain.repository.UsuarioRepository;
import com.concessionaria.gomez.util.DatabaseCleaner;
import com.concessionaria.gomez.util.TokensDeTeste;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Administração de usuários (somente ADMIN): cadastro, alteração, ativação e senha temporária. */
@SpringBootTest
@TestPropertySource("/application-test.properties")
class UsuarioIT {

    private static final String SENHA_VENDEDOR = "senha-do-vendedor-123";
    private static final Pattern VALOR_COOKIE = Pattern.compile("gomez_refresh=([^;]*)");

    @Autowired WebApplicationContext contexto;
    @Autowired DatabaseCleaner cleaner;
    @Autowired UsuarioRepository usuarios;
    @Autowired SessaoUsuarioRepository sessoes;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired TokensDeTeste tokens;

    MockMvc mvc;
    Usuario admin;
    Usuario vendedor;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        cleaner.clearTables();
        // O administrador do token precisa existir no banco para as regras sobre "o próprio usuário"
        admin = salvar("Administradora", TokensDeTeste.EMAIL_ADMIN, PerfilOperadorComercial.ADMIN, "senha-do-admin-123");
        vendedor = salvar("Vendedor Teste", "vendedor.joao@teste.gomez", PerfilOperadorComercial.VENDEDOR, SENHA_VENDEDOR);
    }

    @Test
    @DisplayName("Cadastro gera senha temporária mostrada uma vez (no-store), que funciona no login; hash nunca sai")
    void cadastraUsuarioComSenhaTemporaria() throws Exception {
        MvcResult criado = comoAdmin(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "  Ana Lima ", "email", "Ana.Lima@Teste.Gomez","perfil", "GERENTE"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.usuario.nome").value("Ana Lima"))
                .andExpect(jsonPath("$.usuario.email").value("ana.lima@teste.gomez"))
                .andExpect(jsonPath("$.usuario.perfil").value("GERENTE"))
                .andExpect(jsonPath("$.usuario.ativo").value(true))
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andReturn();

        String senhaTemporaria = corpo(criado).get("senhaTemporaria").asText();
        assertThat(senhaTemporaria).hasSize(16);
        assertThat(usuarios.findByEmailIgnoreCase("ana.lima@teste.gomez").orElseThrow().getSenhaHash())
                .startsWith("{bcrypt}")
                .doesNotContain(senhaTemporaria);

        login("ana.lima@teste.gomez", senhaTemporaria)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.perfil").value("GERENTE"));
    }

    @Test
    @DisplayName("E-mail repetido (ignorando maiúsculas) é recusado com 409; e-mail inválido com 400")
    void emailUnicoEValido() throws Exception {
        comoAdmin(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Outro", "email", "VENDEDOR.JOAO@teste.gomez", "perfil", "VENDEDOR"))))
                .andExpect(status().isConflict());
        comoAdmin(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Outro", "email", "nao-e-email", "perfil", "VENDEDOR"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lista e consulta usuários sem expor o hash da senha")
    void listaEConsulta() throws Exception {
        comoAdmin(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].nome").value("Administradora"))
                .andExpect(jsonPath("$.content[0].senhaHash").doesNotExist());
        comoAdmin(get("/usuarios/" + vendedor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("vendedor.joao@teste.gomez"));
        comoAdmin(get("/usuarios/999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Altera nome e perfil; o novo perfil vale no próximo token emitido (refresh)")
    void alteraNomeEPerfil() throws Exception {
        String cookie = cookieDe(login(vendedor.getEmail(), SENHA_VENDEDOR).andReturn());

        comoAdmin(put("/usuarios/" + vendedor.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "João Gerente", "perfil", "GERENTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Gerente"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"))
                .andExpect(jsonPath("$.email").value("vendedor.joao@teste.gomez"));

        refresh(cookie)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.perfil").value("GERENTE"));
    }

    @Test
    @DisplayName("Administrador não altera o próprio perfil nem inativa a si mesmo, nem gera senha temporária para si")
    void protecoesContraOProprio() throws Exception {
        comoAdmin(put("/usuarios/" + admin.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Admin", "perfil", "VENDEDOR"))))
                .andExpect(status().isBadRequest());
        // Mudar só o próprio nome é permitido
        comoAdmin(put("/usuarios/" + admin.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Admin Renomeada", "perfil", "ADMIN"))))
                .andExpect(status().isOk());
        comoAdmin(delete("/usuarios/" + admin.getId() + "/ativo")).andExpect(status().isBadRequest());
        comoAdmin(post("/usuarios/" + admin.getId() + "/senha-temporaria")).andExpect(status().isBadRequest());

        Usuario depois = usuarios.findById(admin.getId()).orElseThrow();
        assertThat(depois.getPerfil()).isEqualTo(PerfilOperadorComercial.ADMIN);
        assertThat(depois.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Inativar encerra as sessões e impede o login; reativar zera o bloqueio e devolve o acesso")
    void inativaEReativa() throws Exception {
        String cookie = cookieDe(login(vendedor.getEmail(), SENHA_VENDEDOR).andReturn());

        comoAdmin(delete("/usuarios/" + vendedor.getId() + "/ativo")).andExpect(status().isNoContent());

        assertThat(sessoes.findAll()).extracting(SessaoUsuario::getMotivoRevogacao)
                .containsOnly(MotivoRevogacao.USUARIO_INATIVO);
        refresh(cookie).andExpect(status().isUnauthorized());
        login(vendedor.getEmail(), SENHA_VENDEDOR).andExpect(status().isUnauthorized());

        // Bloqueio por tentativas em andamento antes da reativação
        jdbc.update("update usuario set tentativas_falhas = 3, bloqueado_ate = date_add(now(), interval 10 minute) where id = ?",
                vendedor.getId());
        comoAdmin(put("/usuarios/" + vendedor.getId() + "/ativo")).andExpect(status().isNoContent());

        Usuario reativado = usuarios.findById(vendedor.getId()).orElseThrow();
        assertThat(reativado.getAtivo()).isTrue();
        assertThat(reativado.getTentativasFalhas()).isZero();
        assertThat(reativado.getBloqueadoAte()).isNull();
        login(vendedor.getEmail(), SENHA_VENDEDOR).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Senha temporária: troca a senha, zera o bloqueio e encerra todas as sessões do usuário")
    void senhaTemporaria() throws Exception {
        String cookie = cookieDe(login(vendedor.getEmail(), SENHA_VENDEDOR).andReturn());
        jdbc.update("update usuario set tentativas_falhas = 0, bloqueado_ate = date_add(now(), interval 10 minute) where id = ?",
                vendedor.getId());

        MvcResult resultado = comoAdmin(post("/usuarios/" + vendedor.getId() + "/senha-temporaria"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.usuario.id").value(vendedor.getId()))
                .andReturn();
        String temporaria = corpo(resultado).get("senhaTemporaria").asText();

        assertThat(sessoes.findAll()).extracting(SessaoUsuario::getMotivoRevogacao)
                .containsOnly(MotivoRevogacao.SENHA_ALTERADA);
        refresh(cookie).andExpect(status().isUnauthorized());
        login(vendedor.getEmail(), SENHA_VENDEDOR).andExpect(status().isUnauthorized());
        login(vendedor.getEmail(), temporaria).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rotas de usuário em ID inexistente respondem 404")
    void idInexistente() throws Exception {
        comoAdmin(put("/usuarios/999999/ativo")).andExpect(status().isNotFound());
        comoAdmin(delete("/usuarios/999999/ativo")).andExpect(status().isNotFound());
        comoAdmin(post("/usuarios/999999/senha-temporaria")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- utilitários

    private Usuario salvar(String nome, String email, PerfilOperadorComercial perfil, String senha) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        return usuarios.saveAndFlush(usuario);
    }

    private ResultActions comoAdmin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requisicao)
            throws Exception {
        return mvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.admin()));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "senha", senha))));
    }

    private ResultActions refresh(String cookie) throws Exception {
        return mvc.perform(post("/auth/refresh")
                .cookie(new Cookie("gomez_refresh", cookie))
                .header("X-Requested-With", "gomez-spa"));
    }

    private static String cookieDe(MvcResult resultado) {
        Matcher m = VALOR_COOKIE.matcher(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private String json(Object valor) throws Exception {
        return mapper.writeValueAsString(valor);
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        return mapper.readTree(resultado.getResponse().getContentAsString());
    }
}
