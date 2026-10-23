package com.concessionaria.gomez.infrastructure.service.storage;

import com.concessionaria.gomez.core.storage.StorageProperties;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.service.FotoStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DiscoLocalFotoStorageService implements FotoStorageService {

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public void armazenar(NovaFoto novaFoto) {
        try {
            Path arquivoPath = getArquivoPath(novaFoto.getNomeArquivo());
            Files.createDirectories(arquivoPath.getParent());
            FileCopyUtils.copy(novaFoto.getInputStream(), Files.newOutputStream(arquivoPath));
        } catch (Exception e) {
            throw new NegocioException("Não foi possível armazenar o arquivo da foto: " + e.getMessage());
        }
    }

    @Override
    public void remover(String nomeArquivo) {
        try {
            Path arquivoPath = getArquivoPath(nomeArquivo);
            Files.deleteIfExists(arquivoPath);
        } catch (Exception e) {
            throw new NegocioException("Não foi possível excluir o arquivo da foto: " + e.getMessage());
        }
    }

    @Override
    public FotoRecuperada recuperar(String nomeArquivo) {
        try {
            Path arquivoPath = getArquivoPath(nomeArquivo);

            if (!Files.exists(arquivoPath)) {
                return null;
            }

            InputStream inputStream = Files.newInputStream(arquivoPath);

            return FotoRecuperada.builder()
                    .inputStream(inputStream)
                    .build();
        } catch (Exception e) {
            throw new NegocioException("Não foi possível recuperar o arquivo da foto: " + e.getMessage());
        }
    }

    private Path getArquivoPath(String nomeArquivo) {
        return storageProperties.getLocal().getDiretorioFotos().resolve(Path.of(nomeArquivo));
    }
}
