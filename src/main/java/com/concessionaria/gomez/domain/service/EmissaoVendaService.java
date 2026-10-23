package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.exception.VendaNaoEncontradaException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.model.Venda;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class EmissaoVendaService {

    private static final String MSG_VENDA_NAO_ENCONTRADA
            = "Não existe um registro de venda com código %d";

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private CadastroCarroService cadastroCarroService;

    @Autowired
    private CadastroClienteService cadastroClienteService;

    @Transactional
    public Venda realizarVenda(Long carroId, Long clienteId, BigDecimal valorVenda, String observacoes) {
        Carro carro = cadastroCarroService.buscarOuFalhar(carroId);
        Cliente cliente = cadastroClienteService.buscarOuFalhar(clienteId);

        if (!Boolean.TRUE.equals(carro.getAtivo())) {
            throw new NegocioException(
                    String.format("O veículo '%s %s' (código %d) não está disponível para venda.",
                            carro.getMarca(), carro.getModelo(), carro.getId()));
        }

        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new NegocioException(
                    String.format("O cliente '%s' (código %d) está inativo e não pode realizar compras.",
                            cliente.getNome(), cliente.getId()));
        }

        // Regra de negócio: Inativa o carro no estoque ao ser vendido
        carro.inativar();
        cadastroCarroService.salvar(carro);

        Venda venda = new Venda();
        venda.setCarro(carro);
        venda.setCliente(cliente);
        venda.setValorVenda(valorVenda);
        venda.setObservacoes(observacoes);

        return vendaRepository.save(venda);
    }

    public Venda buscarOuFalhar(Long vendaId) {
        return vendaRepository.findById(vendaId)
                .orElseThrow(() -> new VendaNaoEncontradaException(
                        String.format(MSG_VENDA_NAO_ENCONTRADA, vendaId)));
    }

    @Transactional
    public void cancelar(Long vendaId) {
        Venda venda = buscarOuFalhar(vendaId);
        Carro carro = venda.getCarro();

        // Ao cancelar a venda, o carro volta a ficar ativo para venda no estoque
        if (carro != null) {
            carro.ativar();
            cadastroCarroService.salvar(carro);
        }

        vendaRepository.delete(venda);
        vendaRepository.flush();
    }
}
