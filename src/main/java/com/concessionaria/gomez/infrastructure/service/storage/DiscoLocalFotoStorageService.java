package com.concessionaria.gomez.infrastructure.service.storage;

import com.concessionaria.gomez.core.storage.StorageProperties;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.service.FotoStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Guarda as fotos em disco. Falhas de E/S são registradas no log com a causa completa e chegam ao
 * cliente apenas como mensagem genérica: a mensagem original da {@link IOException} traz o caminho
 * absoluto no servidor (SEC-09).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoLocalFotoStorageService implements FotoStorageService {

    static final String MSG_FALHA_ARMAZENAR = "Não foi possível armazenar o arquivo da foto. Tente novamente.";
    static final String MSG_FALHA_REMOVER = "Não foi possível excluir o arquivo da foto. Tente novamente.";
    static final String MSG_FALHA_RECUPERAR = "Não foi possível recuperar o arquivo da foto. Tente novamente.";

    private final StorageProperties storageProperties;

    @Override
    public void armazenar(NovaFoto novaFoto) {
        Path arquivoPath = getArquivoPath(novaFoto.getNomeArquivo());
        try {
            Files.createDirectories(arquivoPath.getParent());
            FileCopyUtils.copy(novaFoto.getInputStream(), Files.newOutputStream(arquivoPath));
        } catch (IOException e) {
            log.error("Falha ao armazenar a foto {}", novaFoto.getNomeArquivo(), e);
            throw new NegocioException(MSG_FALHA_ARMAZENAR);
        }
    }

    @Override
    public void remover(String nomeArquivo) {
        Path arquivoPath = getArquivoPath(nomeArquivo);
        try {
            Files.deleteIfExists(arquivoPath);
        } catch (IOException e) {
            log.error("Falha ao excluir a foto {}", nomeArquivo, e);
            throw new NegocioException(MSG_FALHA_REMOVER);
        }
    }

    @Override
    public FotoRecuperada recuperar(String nomeArquivo) {
        Path arquivoPath = getArquivoPath(nomeArquivo);
        if (!Files.exists(arquivoPath)) {
            return null;
        }
        try {
            InputStream inputStream = Files.newInputStream(arquivoPath);
            return FotoRecuperada.builder()
                    .inputStream(inputStream)
                    .build();
        } catch (IOException e) {
            log.error("Falha ao ler a foto {}", nomeArquivo, e);
            throw new NegocioException(MSG_FALHA_RECUPERAR);
        }
    }

    private Path getArquivoPath(String nomeArquivo) {
        Path raiz = storageProperties.getLocal().getDiretorioFotos().toAbsolutePath().normalize();
        Path arquivo;
        try {
            arquivo = raiz.resolve(nomeArquivo).normalize();
        } catch (InvalidPathException e) {
            throw new NegocioException("Nome de arquivo de foto inválido.");
        }
        if (!arquivo.startsWith(raiz) || arquivo.equals(raiz)) {
            throw new NegocioException("Nome de arquivo de foto inválido.");
        }
        return arquivo;
    }
}
