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
import java.time.OffsetDateTime;

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

        // Regra de negócio: Inativa o carro no estoque e marca como vendido
        carro.marcarComoVendido();
        cadastroCarroService.salvar(carro);

        Venda venda = new Venda();
        venda.setCarro(carro);
        venda.setCliente(cliente);
        venda.setValorVenda(valorVenda);
        venda.setObservacoes(observacoes);

        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda atualizar(Long vendaId, Long novoCarroId, Long novoClienteId, BigDecimal novoValorVenda, String novasObservacoes) {
        Venda venda = buscarOuFalhar(vendaId);
        Carro carroAtual = venda.getCarro();
        Cliente clienteAtual = venda.getCliente();

        // 1. Atualização e validação de cliente se modificado
        if (!clienteAtual.getId().equals(novoClienteId)) {
            Cliente novoCliente = cadastroClienteService.buscarOuFalhar(novoClienteId);
            if (!Boolean.TRUE.equals(novoCliente.getAtivo())) {
                throw new NegocioException(
                        String.format("O cliente '%s' (código %d) está inativo e não pode realizar compras.",
                                novoCliente.getNome(), novoCliente.getId()));
            }
            venda.setCliente(novoCliente);
        }

        // 2. Atualização e transição de veículos no estoque se modificado
        if (!carroAtual.getId().equals(novoCarroId)) {
            Carro novoCarro = cadastroCarroService.buscarOuFalhar(novoCarroId);
            if (!Boolean.TRUE.equals(novoCarro.getAtivo())) {
                throw new NegocioException(
                        String.format("O veículo '%s %s' (código %d) não está disponível para venda.",
                                novoCarro.getMarca(), novoCarro.getModelo(), novoCarro.getId()));
            }

            // O carro anterior é reativado para voltar ao estoque
            carroAtual.ativar();
            cadastroCarroService.salvar(carroAtual);

            // O novo carro é inativado e marcado como vendido no estoque
            novoCarro.marcarComoVendido();
            cadastroCarroService.salvar(novoCarro);

            venda.setCarro(novoCarro);
        }

        // 3. Atualização de valores e observações
        venda.setValorVenda(novoValorVenda);
        venda.setObservacoes(novasObservacoes);

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

    @Transactional
    public Venda cancelarComAuditoria(
            Long vendaId,
            String motivo,
            String responsavel,
            boolean reativarVeiculoNoEstoque) {

        Venda venda = buscarOuFalhar(vendaId);
        if (Boolean.TRUE.equals(venda.getCancelada())) {
            return venda;
        }
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException("O motivo do cancelamento é obrigatório para registro de auditoria.");
        }

        venda.setCancelada(Boolean.TRUE);
        venda.setMotivoCancelamento(motivo.trim());
        venda.setResponsavelCancelamento(responsavel != null && !responsavel.isBlank()
                ? responsavel.trim()
                : "gerente@concessionariagomez.com.br");
        venda.setDataCancelamento(OffsetDateTime.now());

        if (reativarVeiculoNoEstoque && venda.getCarro() != null) {
            Carro carro = venda.getCarro();
            carro.ativar();
            cadastroCarroService.salvar(carro);
        }

        return vendaRepository.save(venda);
    }
}
