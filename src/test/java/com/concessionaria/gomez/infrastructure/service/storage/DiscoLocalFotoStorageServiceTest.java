package com.concessionaria.gomez.infrastructure.service.storage;

import com.concessionaria.gomez.core.storage.StorageProperties;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.service.FotoStorageService.NovaFoto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SEC-09: falhas de disco não podem devolver ao cliente o caminho absoluto do servidor. */
class DiscoLocalFotoStorageServiceTest {

    @TempDir
    Path tempDir;

    private DiscoLocalFotoStorageService servicoCom(Path diretorioFotos) {
        StorageProperties propriedades = new StorageProperties();
        propriedades.getLocal().setDiretorioFotos(diretorioFotos);
        return new DiscoLocalFotoStorageService(propriedades);
    }

    @Test
    void falhaAoArmazenarDevolveMensagemGenericaSemCaminho() throws IOException {
        // O "diretório" de fotos é um arquivo comum: criar a pasta falha com uma IOException que traz o caminho
        Path arquivoComum = Files.createFile(tempDir.resolve("nao-e-pasta"));
        var servico = servicoCom(arquivoComum);
        var foto = NovaFoto.builder()
                .nomeArquivo("foto.jpg")
                .contentType("image/jpeg")
                .inputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}))
                .build();

        assertThatThrownBy(() -> servico.armazenar(foto))
                .isInstanceOf(NegocioException.class)
                .hasMessage(DiscoLocalFotoStorageService.MSG_FALHA_ARMAZENAR)
                .message().doesNotContain(tempDir.toString());
    }

    @Test
    void falhaAoRemoverDevolveMensagemGenericaSemCaminho() throws IOException {
        // Uma pasta não vazia com o nome da foto: deleteIfExists falha com DirectoryNotEmptyException
        Path pasta = Files.createDirectory(tempDir.resolve("foto.jpg"));
        Files.createFile(pasta.resolve("conteudo"));
        var servico = servicoCom(tempDir);

        assertThatThrownBy(() -> servico.remover("foto.jpg"))
                .isInstanceOf(NegocioException.class)
                .hasMessage(DiscoLocalFotoStorageService.MSG_FALHA_REMOVER)
                .message().doesNotContain(tempDir.toString());
    }

    @Test
    void nomeForaDoDiretorioContinuaRecusado() {
        var servico = servicoCom(tempDir);

        assertThatThrownBy(() -> servico.remover("../fora.jpg"))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Nome de arquivo de foto inválido.");
    }

    @Test
    void armazenaERecuperaNormalmente() throws IOException {
        var servico = servicoCom(tempDir);
        servico.armazenar(NovaFoto.builder()
                .nomeArquivo("ok.jpg")
                .contentType("image/jpeg")
                .inputStream(new ByteArrayInputStream(new byte[] {7, 8, 9}))
                .build());

        var recuperada = servico.recuperar("ok.jpg");
        try (var dados = recuperada.getInputStream()) {
            assertThat(dados.readAllBytes()).containsExactly(7, 8, 9);
        }
        assertThat(servico.recuperar("inexistente.jpg")).isNull();
    }
}
