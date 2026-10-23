package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.ClienteNaoEncontradoException;
import com.concessionaria.gomez.domain.exception.EntidadeEmUsoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroClienteService {

    private static final String MSG_CLIENTE_EM_USO
            = "Cliente de código %d não pode ser removido, pois está em uso";

    private static final String MSG_CLIENTE_NAO_ENCONTRADO
            = "Não existe um cadastro de cliente com código %d";

    private static final String MSG_CPF_CNPJ_DUPLICADO
            = "Já existe um cliente cadastrado com o CPF/CNPJ %s";

    @Autowired
    private ClienteRepository clienteRepository;

    @Transactional
    public Cliente salvar(Cliente cliente) {
        validarCpfCnpjDuplicado(cliente);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void excluir(Long clienteId) {
        try {
            clienteRepository.deleteById(clienteId);
            clienteRepository.flush();
        } catch (EmptyResultDataAccessException e) {
            throw new ClienteNaoEncontradoException(
                    String.format(MSG_CLIENTE_NAO_ENCONTRADO, clienteId));
        } catch (DataIntegrityViolationException e) {
            throw new EntidadeEmUsoException(
                    String.format(MSG_CLIENTE_EM_USO, clienteId));
        }
    }

    @Transactional
    public void ativar(Long clienteId) {
        Cliente clienteAtual = buscarOuFalhar(clienteId);
        clienteAtual.ativar();
    }

    @Transactional
    public void inativar(Long clienteId) {
        Cliente clienteAtual = buscarOuFalhar(clienteId);
        clienteAtual.inativar();
    }

    public Cliente buscarOuFalhar(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNaoEncontradoException(
                        String.format(MSG_CLIENTE_NAO_ENCONTRADO, clienteId)));
    }

    private void validarCpfCnpjDuplicado(Cliente cliente) {
        if (cliente.getCpfCnpj() == null) {
            return;
        }

        boolean existe;
        if (cliente.getId() == null) {
            existe = clienteRepository.existsByCpfCnpj(cliente.getCpfCnpj());
        } else {
            existe = clienteRepository.existsByCpfCnpjAndIdNot(cliente.getCpfCnpj(), cliente.getId());
        }

        if (existe) {
            throw new NegocioException(
                    String.format(MSG_CPF_CNPJ_DUPLICADO, cliente.getCpfCnpj()));
        }
    }
}
