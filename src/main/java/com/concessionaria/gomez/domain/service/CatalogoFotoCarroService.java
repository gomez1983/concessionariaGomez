package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.FotoCarroNaoEncontradaException;
import com.concessionaria.gomez.domain.model.FotoCarro;
import com.concessionaria.gomez.domain.repository.FotoCarroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Optional;

@Service
public class CatalogoFotoCarroService {

    @Autowired
    private FotoCarroRepository fotoCarroRepository;

    @Autowired
    private FotoStorageService fotoStorageService;

    @Transactional
    public FotoCarro salvar(FotoCarro foto, InputStream dadosArquivo) {
        Long carroId = foto.getCarroId();
        String nomeNovoArquivo = fotoStorageService.gerarNomeArquivo(foto.getNomeArquivo());
        String nomeArquivoExistente = null;

        Optional<FotoCarro> fotoExistente = fotoCarroRepository.findById(carroId);

        if (fotoExistente.isPresent()) {
            nomeArquivoExistente = fotoExistente.get().getNomeArquivo();
            fotoCarroRepository.delete(fotoExistente.get());
        }

        foto.setNomeArquivo(nomeNovoArquivo);
        foto = fotoCarroRepository.save(foto);
        fotoCarroRepository.flush();

        FotoStorageService.NovaFoto novaFoto = FotoStorageService.NovaFoto.builder()
                .nomeArquivo(foto.getNomeArquivo())
                .contentType(foto.getContentType())
                .inputStream(dadosArquivo)
                .build();

        fotoStorageService.substituir(nomeArquivoExistente, novaFoto);

        return foto;
    }

    public FotoCarro buscarOuFalhar(Long carroId) {
        return fotoCarroRepository.findById(carroId)
                .orElseThrow(() -> new FotoCarroNaoEncontradaException(carroId));
    }

    @Transactional
    public void excluir(Long carroId) {
        FotoCarro foto = buscarOuFalhar(carroId);

        fotoCarroRepository.delete(foto);
        fotoCarroRepository.flush();

        fotoStorageService.remover(foto.getNomeArquivo());
    }
}
