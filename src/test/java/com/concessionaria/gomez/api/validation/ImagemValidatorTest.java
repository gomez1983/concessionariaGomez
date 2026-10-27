package com.concessionaria.gomez.api.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ImagemValidatorTest {

    private final ImagemValidator validator = new ImagemValidator();

    // WebP 1x1 pixel válido codificado em Base64
    private static final String WEBP_1X1_BASE64 =
            "UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA=";

    @Test
    @DisplayName("Deve aceitar imagem PNG válida")
    void deveAceitarPngValido() throws IOException {
        byte[] bytes = gerarImagemBytes("PNG");
        MockMultipartFile file = new MockMultipartFile("arquivo", "teste.png", "image/png", bytes);

        boolean valido = validator.isValid(file, null);

        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar imagem JPEG válida")
    void deveAceitarJpegValido() throws IOException {
        byte[] bytes = gerarImagemBytes("JPEG");
        MockMultipartFile file = new MockMultipartFile("arquivo", "teste.jpg", "image/jpeg", bytes);

        boolean valido = validator.isValid(file, null);

        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar imagem WebP válida")
    void deveAceitarWebpValido() {
        byte[] bytes = Base64.getDecoder().decode(WEBP_1X1_BASE64);
        MockMultipartFile file = new MockMultipartFile("arquivo", "teste.webp", "image/webp", bytes);

        boolean valido = validator.isValid(file, null);

        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar formato não permitido como GIF")
    void deveRejeitarGif() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "teste.gif", "image/gif", new byte[]{1, 2, 3});

        boolean valido = validator.isValid(file, null);

        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar arquivo corrompido ou falso WebP")
    void deveRejeitarFalsoWebp() {
        byte[] fakeBytes = "isto nao e uma imagem webp".getBytes();
        MockMultipartFile file = new MockMultipartFile("arquivo", "fake.webp", "image/webp", fakeBytes);

        boolean valido = validator.isValid(file, null);

        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Deve aceitar quando arquivo for nulo (delega para @NotNull)")
    void deveRetornarTrueParaArquivoNulo() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar arquivo vazio")
    void deveRejeitarArquivoVazio() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "vazio.webp", "image/webp", new byte[0]);
        assertThat(validator.isValid(file, null)).isFalse();
    }

    private byte[] gerarImagemBytes(String formato) throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean escrito = ImageIO.write(img, formato, baos);
        if (!escrito) {
            throw new IllegalStateException("Nenhum writer encontrado no ImageIO para o formato: " + formato);
        }
        return baos.toByteArray();
    }
}
