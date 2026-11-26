package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.util.DatabaseCleaner;
import com.concessionaria.gomez.util.TokensDeTeste;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Verifica a matriz de permissões (seção 5 do design da Etapa 3) rota a rota, para cada perfil.
 * "Permitido" significa que a requisição passou pela camada de segurança e foi tratada: o status pode ser
 * 200, 400, 404 ou 415 (IDs inexistentes e corpo vazio), mas nunca 401, 403 ou um erro interno (5xx).
 * <p>
 * As requisições da matriz não gravam dados de negócio (IDs inexistentes e corpo inválido), então o banco é
 * limpo e o MockMvc é montado uma única vez para a classe inteira.
 */
@SpringBootTest
@TestPropertySource("/application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SegurancaMatrizIT {

    private static final String ID = "999999";

    private static final Set<PerfilOperadorComercial> TODOS = EnumSet.allOf(PerfilOperadorComercial.class);
    private static final Set<PerfilOperadorComercial> GESTAO =
            EnumSet.of(PerfilOperadorComercial.GERENTE, PerfilOperadorComercial.ADMIN);
    private static final Set<PerfilOperadorComercial> ADMIN = EnumSet.of(PerfilOperadorComercial.ADMIN);

    @Autowired WebApplicationContext contexto;
    @Autowired TokensDeTeste tokens;
    @Autowired DatabaseCleaner cleaner;

    MockMvc mvc;

    @BeforeAll
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        cleaner.clearTables();
    }

    static Stream<Arguments> matriz() {
        return Stream.of(
                // Carros
                rota("GET", "/carros", TODOS),
                rota("GET", "/carros/" + ID, TODOS),
                rota("POST", "/carros", GESTAO),
                rota("PUT", "/carros/" + ID, GESTAO),
                rota("PUT", "/carros/" + ID + "/ativo", GESTAO),
                rota("DELETE", "/carros/" + ID + "/ativo", GESTAO),
                rota("DELETE", "/carros/" + ID, ADMIN),
                // Foto principal e galeria
                rota("GET", "/carros/" + ID + "/foto", TODOS),
                rota("PUT", "/carros/" + ID + "/foto", TODOS),
                rota("DELETE", "/carros/" + ID + "/foto", GESTAO),
                rota("GET", "/carros/" + ID + "/fotos", TODOS),
                rota("POST", "/carros/" + ID + "/fotos", TODOS),
                rota("PUT", "/carros/" + ID + "/fotos/" + ID, TODOS),
                rota("GET", "/carros/" + ID + "/fotos/" + ID + "/conteudo", TODOS),
                rota("DELETE", "/carros/" + ID + "/fotos/" + ID, GESTAO),
                // Clientes
                rota("GET", "/clientes", TODOS),
                rota("GET", "/clientes/" + ID, TODOS),
                rota("POST", "/clientes", TODOS),
                rota("PUT", "/clientes/" + ID, TODOS),
                rota("PUT", "/clientes/" + ID + "/ativo", GESTAO),
                rota("DELETE", "/clientes/" + ID + "/ativo", GESTAO),
                rota("DELETE", "/clientes/" + ID, ADMIN),
                // Dashboard
                rota("GET", "/dashboard", TODOS),
                // Propostas
                rota("GET", "/propostas", TODOS),
                rota("GET", "/propostas/" + ID, TODOS),
                rota("POST", "/propostas", TODOS),
                rota("POST", "/propostas/simulacoes", TODOS),
                rota("POST", "/propostas/" + ID + "/revisoes", TODOS),
                rota("POST", "/propostas/" + ID + "/reservar", TODOS),
                rota("POST", "/propostas/" + ID + "/converter-venda", TODOS),
                rota("POST", "/propostas/" + ID + "/aprovacoes", GESTAO),
                rota("GET", "/propostas/politica-vigente", GESTAO),
                rota("POST", "/propostas/politicas", GESTAO),
                // Vendas
                rota("GET", "/vendas", TODOS),
                rota("GET", "/vendas/" + ID, TODOS),
                rota("POST", "/vendas", GESTAO),
                rota("PUT", "/vendas/" + ID, GESTAO),
                rota("POST", "/vendas/" + ID + "/cancelamento-auditado", GESTAO),
                rota("DELETE", "/vendas/" + ID, ADMIN),
                // Usuário autenticado
                rota("GET", "/auth/me", TODOS),
                rota("PUT", "/auth/senha", TODOS),
                // Administração de usuários
                rota("GET", "/usuarios", ADMIN),
                rota("GET", "/usuarios/" + ID, ADMIN),
                rota("POST", "/usuarios", ADMIN),
                rota("PUT", "/usuarios/" + ID, ADMIN),
                rota("PUT", "/usuarios/" + ID + "/ativo", ADMIN),
                rota("DELETE", "/usuarios/" + ID + "/ativo", ADMIN),
                rota("POST", "/usuarios/" + ID + "/senha-temporaria", ADMIN)
        );
    }

    private static Arguments rota(String metodo, String caminho, Set<PerfilOperadorComercial> permitidos) {
        return Arguments.of(metodo, caminho, permitidos);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("matriz")
    @DisplayName("Cada perfil só passa pelas rotas que a matriz permite; sem token, toda rota protegida responde 401")
    void respeitaMatriz(String metodo, String caminho, Set<PerfilOperadorComercial> permitidos) throws Exception {
        int semToken = mvc.perform(requisicao(metodo, caminho)).andReturn().getResponse().getStatus();
        assertThat(semToken).as("%s %s sem token", metodo, caminho).isEqualTo(401);

        for (PerfilOperadorComercial perfil : PerfilOperadorComercial.values()) {
            int status = mvc.perform(requisicao(metodo, caminho)
                            .header(HttpHeaders.AUTHORIZATION, tokens.bearer(perfil)))
                    .andReturn().getResponse().getStatus();

            if (permitidos.contains(perfil)) {
                assertThat(status).as("%s %s como %s deveria passar pela segurança sem erro interno", metodo, caminho, perfil)
                        .isNotIn(401, 403)
                        .isLessThan(500);
            } else {
                assertThat(status).as("%s %s como %s deveria ser negado", metodo, caminho, perfil)
                        .isEqualTo(403);
            }
        }
    }

    @Test
    @DisplayName("Rotas públicas: login, refresh, logout e documentação não exigem token")
    void rotasPublicas() throws Exception {
        for (String caminho : new String[]{"/auth/login", "/auth/refresh", "/auth/logout"}) {
            int status = mvc.perform(requisicao("POST", caminho)).andReturn().getResponse().getStatus();
            assertThat(status).as("POST %s", caminho).isNotEqualTo(401);
        }
        assertThat(mvc.perform(requisicao("GET", "/v3/api-docs")).andReturn().getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Rota sem regra explícita falha fechada (403) mesmo para ADMIN")
    void rotaDesconhecidaFalhaFechada() throws Exception {
        int status = mvc.perform(requisicao("GET", "/rota-que-nao-existe")
                        .header(HttpHeaders.AUTHORIZATION, tokens.bearer(PerfilOperadorComercial.ADMIN)))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(403);
    }

    @Test
    @DisplayName("Os cabeçalhos antigos X-Operador-* não autenticam ninguém")
    void cabecalhosAntigosNaoAutenticam() throws Exception {
        int status = mvc.perform(requisicao("GET", "/dashboard")
                        .header("X-Operador-Perfil", "ADMIN")
                        .header("X-Operador-Id", "admin@concessionariagomez.com.br"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }

    private static MockHttpServletRequestBuilder requisicao(String metodo, String caminho) {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(metodo), caminho);
        if (!metodo.equals("GET") && !metodo.equals("DELETE")) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return builder;
    }
}
