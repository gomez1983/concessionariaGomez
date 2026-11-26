package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.util.DatabaseCleaner;
import com.concessionaria.gomez.util.TokensDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/application-test.properties")
class PoliticaComercialIT {

    private static final String AUTH = "Authorization";

    @Autowired MockMvc mvc;
    @Autowired CarroRepository carros;
    @Autowired DatabaseCleaner cleaner;
    @Autowired TokensDeTeste tokens;

    Long carroId;

    @BeforeEach
    void setup() {
        cleaner.clearTables();
        Carro carro = new Carro();
        carro.setMarca("Teste");
        carro.setModelo("Politica");
        carro.setAno(2024);
        carro.setCompra(new BigDecimal("50000.00"));
        carro.setQuilometragem(0);
        carroId = carros.saveAndFlush(carro).getId();
    }

    @Test
    @DisplayName("VENDEDOR não consulta as margens da política comercial (403) e chamada sem token recebe 401")
    void vendedorNaoDeveConsultarPoliticaVigente() throws Exception {
        mvc.perform(get("/propostas/politica-vigente").header(AUTH, "Bearer " + tokens.vendedor()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/propostas/politica-vigente").header("X-Operador-Perfil", "GERENTE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Política de exceção por veículo deve ser devolvida como DTO, só com o ID do veículo")
    void politicaDeExcecaoDeveSerDevolvidaSemExporOVeiculo() throws Exception {
        String politica = """
                {"contexto":"EXCECAO_VEICULO","carroId":%d,
                 "margemDesejada":0.12,"margemAutonomia":0.08,"margemPisoAbsoluto":0.03}
                """.formatted(carroId);

        mvc.perform(post("/propostas/politicas").header(AUTH, "Bearer " + tokens.gerente())
                        .contentType(MediaType.APPLICATION_JSON).content(politica))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carroId").value(carroId))
                .andExpect(jsonPath("$.responsavelCadastro").value(TokensDeTeste.EMAIL_GERENTE))
                .andExpect(jsonPath("$.carro").doesNotExist());

        // Nova requisição: a política é carregada do banco com o veículo como proxy LAZY
        mvc.perform(get("/propostas/politica-vigente").param("carroId", carroId.toString())
                        .header(AUTH, "Bearer " + tokens.gerente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexto").value("EXCECAO_VEICULO"))
                .andExpect(jsonPath("$.carroId").value(carroId))
                .andExpect(jsonPath("$.margemPisoAbsoluto").value(0.03))
                .andExpect(jsonPath("$.carro").doesNotExist())
                .andExpect(jsonPath("$..compra").isEmpty());
    }
}
