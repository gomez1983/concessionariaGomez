package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.FotoCarroNaoEncontradaException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.FotoCarro;
import com.concessionaria.gomez.domain.repository.FotoCarroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoFotoCarroServiceTest {

    @Mock
    private FotoCarroRepository fotoCarroRepository;

    @Mock
    private ArquivoFotoTransacionalService arquivos;

    @InjectMocks
    private CatalogoFotoCarroService catalogoFotoCarroService;

    @Test
    @DisplayName("Deve salvar foto nova com sucesso")
    void deveSalvarFotoNova() {
        Carro carro = new Carro();
        carro.setId(1L);

        FotoCarro foto = new FotoCarro();
        foto.setCarro(carro);
        foto.setNomeArquivo("ferrari.jpg");
        foto.setContentType("image/jpeg");
        foto.setTamanho(1024L);

        when(fotoCarroRepository.findById(1L)).thenReturn(Optional.empty());
        when(arquivos.armazenar(eq("image/jpeg"), any(InputStream.class))).thenReturn("uuid_ferrari.jpg");
        when(fotoCarroRepository.save(any(FotoCarro.class))).thenAnswer(inv -> inv.getArgument(0));

        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        FotoCarro fotoSalva = catalogoFotoCarroService.salvar(foto, stream);

        assertThat(fotoSalva).isNotNull();
        assertThat(fotoSalva.getNomeArquivo()).isEqualTo("uuid_ferrari.jpg");
        verify(fotoCarroRepository).save(foto);
        verify(arquivos, never()).removerAposCommit(any());
    }

    @Test
    @DisplayName("Deve substituir foto existente e remover arquivo antigo")
    void deveSubstituirFotoExistente() {
        Carro carro = new Carro();
        carro.setId(1L);

        FotoCarro fotoExistente = new FotoCarro();
        fotoExistente.setId(1L);
        fotoExistente.setNomeArquivo("antiga.jpg");

        FotoCarro novaFoto = new FotoCarro();
        novaFoto.setCarro(carro);
        novaFoto.setNomeArquivo("nova.jpg");
        novaFoto.setContentType("image/jpeg");
        novaFoto.setTamanho(2048L);

        when(fotoCarroRepository.findById(1L)).thenReturn(Optional.of(fotoExistente));
        when(arquivos.armazenar(eq("image/jpeg"), any(InputStream.class))).thenReturn("uuid_nova.jpg");
        when(fotoCarroRepository.save(any(FotoCarro.class))).thenAnswer(inv -> inv.getArgument(0));

        InputStream stream = new ByteArrayInputStream(new byte[]{4, 5, 6});
        FotoCarro fotoSalva = catalogoFotoCarroService.salvar(novaFoto, stream);

        assertThat(fotoSalva.getNomeArquivo()).isEqualTo("uuid_nova.jpg");
        verify(fotoCarroRepository, never()).delete(any());
        verify(arquivos).removerAposCommit("antiga.jpg");
    }

    @Test
    @DisplayName("Deve lançar FotoCarroNaoEncontradaException quando não existir foto para o carro")
    void deveLancarExcecaoQuandoFotoNaoEncontrada() {
        when(fotoCarroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogoFotoCarroService.buscarOuFalhar(99L))
                .isInstanceOf(FotoCarroNaoEncontradaException.class)
                .hasMessageContaining("Não existe um cadastro de foto para o veículo com código 99");
    }

    @Test
    @DisplayName("Deve excluir foto existente e remover arquivo do storage")
    void deveExcluirFotoERemoverDoStorage() {
        FotoCarro foto = new FotoCarro();
        foto.setId(5L);
        foto.setNomeArquivo("excluir.jpg");

        when(fotoCarroRepository.findById(5L)).thenReturn(Optional.of(foto));

        catalogoFotoCarroService.excluir(5L);

        verify(fotoCarroRepository).delete(foto);
        verify(arquivos).removerAposCommit("excluir.jpg");
    }
}
