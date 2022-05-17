package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.CarroNaoEncontradoException;
import com.concessionaria.gomez.domain.exception.EntidadeEmUsoException;
import com.concessionaria.gomez.domain.exception.EntidadeNaoEncontradaException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class CadastroCarroService {

    /********Começo de mensagens CONSTANTS *********/

    private static final String MSG_CARRO_EM_USO
            = "Carro de código %d não pode ser removido, pois está em uso";

    private static final String MSG_CARRO_NAO_ENCONTRADO
            = "Não existe um cadastro de carro com código %d";

    /********Fim de mensagens CONSTANTS *********/

    @Autowired
    private CarroRepository carroRepository;

    public Carro salvar(Carro carro) {
        return carroRepository.save(carro);
    }

    public void excluir(Long carroId) {
        try {
            carroRepository.deleteById(carroId);
            carroRepository.flush();

        } catch (EmptyResultDataAccessException e) {
            throw new EntidadeNaoEncontradaException(
                    String.format("Não existe um cadastro de carro com código %d", carroId));

        } catch (DataIntegrityViolationException e) {
            throw new EntidadeEmUsoException(
                    String.format("Carro de código %d não pode ser removido, pois está em uso", carroId));
        }
    }

    public Carro buscarOuFalhar(Long carroId) {
        return carroRepository.findById(carroId)
                .orElseThrow(() -> new CarroNaoEncontradoException(
                        String.format(MSG_CARRO_NAO_ENCONTRADO,carroId)));
    }
}