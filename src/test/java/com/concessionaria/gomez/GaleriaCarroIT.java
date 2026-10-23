package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.util.DatabaseCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/application-test.properties")
class GaleriaCarroIT {
    @Autowired MockMvc mvc;
    @Autowired CarroRepository carros;
    @Autowired com.concessionaria.gomez.domain.repository.ClienteRepository clientes;
    @Autowired DatabaseCleaner cleaner;
    @Autowired ObjectMapper mapper;
    @TempDir static Path fotos;
    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry) {
        registry.add("concessionaria.storage.local.diretorio-fotos", () -> fotos.toString());
    }
    Long carroId;

    @BeforeEach void setup() {
        cleaner.clearTables();
        Carro carro = new Carro();
        carro.setMarca("Teste"); carro.setModelo("Galeria"); carro.setAno(2024);
        carro.setCompra(new BigDecimal("12345.67")); carro.setQuilometragem(0);
        carroId = carros.saveAndFlush(carro).getId();
    }

    @Test void devePersistirGaleriaSubstituirFotosEServirBinarios() throws Exception {
        byte[] primeira = imagem(0xff334455), segunda = imagem(0xffaabbcc);
        mvc.perform(multipart("/carros/{id}/foto", carroId).file(arquivo(primeira)).param("descricao", "Frente")
                .with(request -> { request.setMethod("PUT"); return request; })).andExpect(status().isOk());
        long fotoId = adicionar(primeira, "Interior");
        adicionar(segunda, "Traseira");
        mvc.perform(get("/carros/{id}/fotos", carroId)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].links[0].href").value("http://localhost/carros/" + carroId + "/fotos/" + fotoId + "/conteudo"));
        mvc.perform(get("/carros/{id}/fotos/{fotoId}/conteudo", carroId, fotoId)).andExpect(content().bytes(primeira));
        mvc.perform(multipart("/carros/{id}/fotos/{fotoId}", carroId, fotoId).file(arquivo(segunda)).param("descricao", "Painel")
                .with(request -> { request.setMethod("PUT"); return request; })).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(fotoId));
        mvc.perform(get("/carros/{id}/fotos/{fotoId}/conteudo", carroId, fotoId)).andExpect(content().bytes(segunda));
        mvc.perform(get("/carros/{id}/fotos/{fotoId}/conteudo", carroId + 1, fotoId)).andExpect(status().isNotFound());
        mvc.perform(multipart("/carros/{id}/foto", carroId).file(arquivo(segunda)).param("descricao", "Nova principal")
                .with(request -> { request.setMethod("PUT"); return request; })).andExpect(status().isOk());
        mvc.perform(get("/carros/{id}/foto", carroId).accept("image/png")).andExpect(content().bytes(segunda));
        mvc.perform(delete("/carros/{id}/fotos/{fotoId}", carroId, fotoId)).andExpect(status().isNoContent());
        mvc.perform(get("/carros/{id}/fotos", carroId)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/carros/{id}/foto", carroId)).andExpect(status().isNoContent());
        mvc.perform(get("/carros/{id}/foto", carroId).accept("application/json")).andExpect(status().isNotFound());
    }

    @Test void deveRejeitarArquivoDisfarcadoSemGravarFoto() throws Exception {
        var falso = new MockMultipartFile("arquivo", "foto.png", "image/png", "isto não é imagem".getBytes());
        mvc.perform(multipart("/carros/{id}/fotos", carroId).file(falso).param("descricao", "Interior"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.objects[0].name").value("arquivo"));
        mvc.perform(get("/carros/{id}/fotos", carroId)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void deveCalcularResumoNoDominioIndependentementeDaPagina() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVeiculos").value(1)).andExpect(jsonPath("$.ativos").value(1))
                .andExpect(jsonPath("$.valorInvestido").value(12345.67)).andExpect(jsonPath("$.vendaSugerida").value(13580.24))
                .andExpect(jsonPath("$.margemPrevista").value(1234.57));
    }

    @Test void deveCompararDocumentosComESemMascaraInclusiveNaEdicao() {
        var cliente = new com.concessionaria.gomez.domain.model.Cliente();
        cliente.setNome("Cliente de teste"); cliente.setCpfCnpj("12345678901");
        cliente = clientes.saveAndFlush(cliente);
        assertThat(clientes.existsByCpfCnpj("123.456.789-01")).isTrue();
        assertThat(clientes.existsByCpfCnpjAndIdNot("123.456.789-01", cliente.getId())).isFalse();
        assertThat(clientes.existsByCpfCnpjAndIdNot("123.456.789-01", cliente.getId() + 1)).isTrue();
        cliente.setCpfCnpj("12.345.678/0001-99"); clientes.saveAndFlush(cliente);
        assertThat(clientes.existsByCpfCnpj("12345678000199")).isTrue();
    }

    private long adicionar(byte[] bytes, String descricao) throws Exception {
        String body = mvc.perform(multipart("/carros/{id}/fotos", carroId).file(arquivo(bytes)).param("descricao", descricao))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }
    private MockMultipartFile arquivo(byte[] bytes) { return new MockMultipartFile("arquivo", "teste.png", "image/png", bytes); }
    private byte[] imagem(int color) throws Exception {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, color);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out); return out.toByteArray();
    }
}
