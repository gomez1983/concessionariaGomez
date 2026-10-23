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
    private ArquivoFotoTransacionalService arquivos;

    @Transactional
    public FotoCarro salvar(FotoCarro foto, InputStream dadosArquivo) {
        Long carroId = foto.getCarroId();
        String nomeNovoArquivo = arquivos.armazenar(foto.getContentType(), dadosArquivo);
        String nomeArquivoExistente = null;

        Optional<FotoCarro> fotoExistente = fotoCarroRepository.findById(carroId);

        if (fotoExistente.isPresent()) {
            nomeArquivoExistente = fotoExistente.get().getNomeArquivo();
            FotoCarro existente = fotoExistente.get();
            existente.setDescricao(foto.getDescricao());
            existente.setContentType(foto.getContentType());
            existente.setTamanho(foto.getTamanho());
            foto = existente;
        }

        foto.setNomeArquivo(nomeNovoArquivo);
        foto = fotoCarroRepository.save(foto);
        fotoCarroRepository.flush();

        if (nomeArquivoExistente != null) arquivos.removerAposCommit(nomeArquivoExistente);

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

        arquivos.removerAposCommit(foto.getNomeArquivo());
    }
}
