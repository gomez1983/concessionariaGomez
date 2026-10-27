package com.concessionaria.gomez.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/** Coordena o disco com a transação: preserva arquivos antigos até o commit. */
@Service
@RequiredArgsConstructor
public class ArquivoFotoTransacionalService {
    private static final Logger LOG = LoggerFactory.getLogger(ArquivoFotoTransacionalService.class);
    private final FotoStorageService storage;

    public String armazenar(String contentType, InputStream stream) {
        String extensao = "image/webp".equals(contentType) ? "foto.webp"
                : ("image/png".equals(contentType) ? "foto.png" : "foto.jpg");
        String nome = storage.gerarNomeArquivo(extensao);
        // O nome original não participa do caminho físico.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) removerComLog(nome);
            }
        });
        storage.armazenar(FotoStorageService.NovaFoto.builder()
                .nomeArquivo(nome).contentType(contentType).inputStream(stream).build());
        return nome;
    }

    public void removerAposCommit(String nome) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { removerComLog(nome); }
        });
    }

    private void removerComLog(String nome) {
        try { storage.remover(nome); }
        catch (RuntimeException ex) { LOG.error("Falha na limpeza do arquivo de foto {}", nome, ex); }
    }
}
