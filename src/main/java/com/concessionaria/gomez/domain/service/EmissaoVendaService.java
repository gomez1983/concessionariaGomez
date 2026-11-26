package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.ConflitoNegociacaoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.exception.VendaNaoEncontradaException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.model.DisponibilidadeComercial;
import com.concessionaria.gomez.domain.model.Venda;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class EmissaoVendaService {

    private static final String MSG_VENDA_NAO_ENCONTRADA
            = "Não existe um registro de venda com código %d";

    /** Tamanho da coluna {@code venda.motivo_cancelamento}. */
    private static final int TAMANHO_MAXIMO_MOTIVO = 255;

    private final VendaRepository vendaRepository;
    private final CadastroCarroService cadastroCarroService;
    private final CadastroClienteService cadastroClienteService;

    @Transactional
    public Venda realizarVenda(Long carroId, Long clienteId, BigDecimal valorVenda, String observacoes) {
        Carro carro = cadastroCarroService.buscarOuFalhar(carroId);
        Cliente cliente = cadastroClienteService.buscarOuFalhar(clienteId);

        if (!Boolean.TRUE.equals(carro.getAtivo())) {
            throw new NegocioException(
                    String.format("O veículo '%s %s' (código %d) não está disponível para venda.",
                            carro.getMarca(), carro.getModelo(), carro.getId()));
        }
        validarSemReservaVigente(carro);

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
        if (Boolean.TRUE.equals(venda.getCancelada())) {
            throw new NegocioException("Vendas canceladas não podem ser alteradas.");
        }
        if (venda.getRevisaoConvertida() != null) {
            throw new NegocioException(
                    "Vendas originadas de proposta aprovada têm valores congelados e não podem ser alteradas. "
                            + "Para mudar a negociação, cancele a venda e gere uma nova revisão da proposta.");
        }

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
            validarSemReservaVigente(novoCarro);

            // O carro anterior volta ao estoque (se não houver outra venda vigente para ele)
            devolverAoEstoque(carroAtual, venda.getId());

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

    /**
     * Exclusão física da venda (legado). Preferir {@link #cancelarComAuditoria}, que preserva a trilha.
     */
    @Transactional
    public void cancelar(Long vendaId) {
        Venda venda = buscarOuFalhar(vendaId);
        // Uma venda já cancelada com auditoria devolveu o veículo ao estoque, que pode ter sido
        // revendido depois. Excluí-la reativaria esse veículo indevidamente (venda dupla).
        if (Boolean.TRUE.equals(venda.getCancelada())) {
            throw new NegocioException(
                    "Esta venda já foi cancelada com auditoria e não pode ser excluída.");
        }
        if (venda.getRevisaoConvertida() != null) {
            throw new NegocioException(
                    "Vendas originadas de proposta aprovada não podem ser excluídas. Use o cancelamento auditado.");
        }
        Carro carro = venda.getCarro();

        // Ao cancelar a venda, o carro volta a ficar ativo para venda no estoque
        if (carro != null) {
            devolverAoEstoque(carro, venda.getId());
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
        if (motivo.trim().length() > TAMANHO_MAXIMO_MOTIVO) {
            throw new NegocioException(String.format(
                    "O motivo do cancelamento deve ter no máximo %d caracteres.", TAMANHO_MAXIMO_MOTIVO));
        }

        if (reativarVeiculoNoEstoque && venda.getCarro() != null) {
            devolverAoEstoque(venda.getCarro(), venda.getId());
        }

        venda.setCancelada(Boolean.TRUE);
        venda.setMotivoCancelamento(motivo.trim());
        // O responsável vem do token autenticado; sem ele não há trilha de auditoria válida
        if (responsavel == null || responsavel.isBlank()) {
            throw new IllegalStateException("Responsável pelo cancelamento não identificado.");
        }
        venda.setResponsavelCancelamento(responsavel.trim());
        venda.setDataCancelamento(OffsetDateTime.now());

        return vendaRepository.save(venda);
    }

    /**
     * Devolve o veículo ao estoque somente se nenhuma outra venda vigente o mantiver vendido.
     * Impede que o cancelamento de uma venda antiga reabra um veículo já revendido (venda dupla).
     */
    private void devolverAoEstoque(Carro carro, Long vendaIgnoradaId) {
        if (vendaRepository.existsByCarroIdAndCanceladaFalseAndIdNot(carro.getId(), vendaIgnoradaId)) {
            throw new NegocioException(
                    String.format("O veículo '%s %s' (código %d) possui outra venda vigente e não pode voltar ao estoque.",
                            carro.getMarca(), carro.getModelo(), carro.getId()));
        }
        carro.ativar();
        cadastroCarroService.salvar(carro);
    }

    private void validarSemReservaVigente(Carro carro) {
        if (carro.getDisponibilidadeComercial() == DisponibilidadeComercial.RESERVADO
                && carro.getReservaValidaAte() != null
                && carro.getReservaValidaAte().isAfter(OffsetDateTime.now())) {
            throw new ConflitoNegociacaoException(String.format(
                    "O veículo '%s %s' está reservado para a proposta %s. Conclua a venda pela proposta ou aguarde o fim da reserva.",
                    carro.getMarca(), carro.getModelo(), carro.getReservaResponsavel()));
        }
    }
}
