package com.concessionaria.gomez.domain.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.ByteArrayInputStream;
import static org.mockito.Mockito.*;

class ArquivoFotoTransacionalServiceTest {
    FotoStorageService storage = mock(FotoStorageService.class);
    ArquivoFotoTransacionalService service = new ArquivoFotoTransacionalService(storage);
    @BeforeEach void iniciar() { TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void limpar() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test void rollbackDeveRemoverNovoArquivoEPreservarAntigo() {
        when(storage.gerarNomeArquivo("foto.png")).thenReturn("novo.png");
        service.armazenar("image/png", new ByteArrayInputStream(new byte[0]));
        service.removerAposCommit("antigo.png");
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).remover("novo.png"); verify(storage, never()).remover("antigo.png");
    }
    @Test void commitDevePreservarNovoArquivoERemoverAntigo() {
        when(storage.gerarNomeArquivo("foto.png")).thenReturn("novo.png");
        service.armazenar("image/png", new ByteArrayInputStream(new byte[0]));
        service.removerAposCommit("antigo.png");
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage).remover("antigo.png"); verify(storage, never()).remover("novo.png");
    }
    @Test void deveGerarExtensaoWebpAoArmazenarImagemWebp() {
        when(storage.gerarNomeArquivo("foto.webp")).thenReturn("uuid_foto.webp");
        service.armazenar("image/webp", new ByteArrayInputStream(new byte[0]));
        verify(storage).gerarNomeArquivo("foto.webp");
        verify(storage).armazenar(argThat(novaFoto ->
                "uuid_foto.webp".equals(novaFoto.getNomeArquivo()) &&
                "image/webp".equals(novaFoto.getContentType())
        ));
    }
}
