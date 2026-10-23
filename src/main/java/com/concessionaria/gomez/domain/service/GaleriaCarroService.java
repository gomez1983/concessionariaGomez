package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.EntidadeNaoEncontradaException;
import com.concessionaria.gomez.domain.model.FotoGaleriaCarro;
import com.concessionaria.gomez.domain.repository.FotoGaleriaCarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GaleriaCarroService {
    private final FotoGaleriaCarroRepository repository;
    private final CadastroCarroService cadastroCarro;
    private final ArquivoFotoTransacionalService arquivos;

    @Transactional(readOnly = true)
    public List<FotoGaleriaCarro> listar(Long carroId) {
        cadastroCarro.buscarOuFalhar(carroId);
        return repository.findByCarroIdOrderByIdAsc(carroId);
    }

    public FotoGaleriaCarro buscarOuFalhar(Long carroId, Long fotoId) {
        return repository.findByIdAndCarroId(fotoId, carroId).orElseThrow(() ->
                new EntidadeNaoEncontradaException("A foto solicitada não pertence à galeria deste veículo."));
    }

    @Transactional
    public FotoGaleriaCarro salvar(Long carroId, Long fotoId, String descricao,
                                   String contentType, long tamanho, InputStream dados) {
        FotoGaleriaCarro foto;
        String antigo = null;
        if (fotoId == null) {
            foto = new FotoGaleriaCarro();
            foto.setCarro(cadastroCarro.buscarOuFalhar(carroId));
        } else {
            foto = buscarOuFalhar(carroId, fotoId);
            antigo = foto.getNomeArquivo();
        }
        foto.setNomeArquivo(arquivos.armazenar(contentType, dados));
        foto.setDescricao(descricao);
        foto.setContentType(contentType);
        foto.setTamanho(tamanho);
        FotoGaleriaCarro salva = repository.saveAndFlush(foto);
        if (antigo != null) arquivos.removerAposCommit(antigo);
        return salva;
    }

    @Transactional
    public void excluir(Long carroId, Long fotoId) {
        FotoGaleriaCarro foto = buscarOuFalhar(carroId, fotoId);
        repository.delete(foto);
        repository.flush();
        arquivos.removerAposCommit(foto.getNomeArquivo());
    }
}
