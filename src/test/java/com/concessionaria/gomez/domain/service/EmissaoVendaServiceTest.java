package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.ConflitoNegociacaoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.exception.VendaNaoEncontradaException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.model.DisponibilidadeComercial;
import com.concessionaria.gomez.domain.model.RevisaoProposta;
import com.concessionaria.gomez.domain.model.Venda;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmissaoVendaServiceTest {

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private CadastroCarroService cadastroCarroService;

    @Mock
    private CadastroClienteService cadastroClienteService;

    @InjectMocks
    private EmissaoVendaService emissaoVendaService;

    @Test
    @DisplayName("Deve realizar venda com sucesso e inativar o veículo no estoque")
    void deveRealizarVendaComSucessoEInativarVeiculo() {
        Carro carro = new Carro();
        carro.setId(1L);
        carro.setMarca("Toyota");
        carro.setModelo("Corolla");
        carro.setAtivo(true);

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setNome("Comprador Satisfeito");
        cliente.setAtivo(true);

        when(cadastroCarroService.buscarOuFalhar(1L)).thenReturn(carro);
        when(cadastroClienteService.buscarOuFalhar(10L)).thenReturn(cliente);
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });

        Venda venda = emissaoVendaService.realizarVenda(1L, 10L, new BigDecimal("115000.00"), "Venda com desconto à vista");

        assertThat(venda).isNotNull();
        assertThat(venda.getId()).isEqualTo(99L);
        assertThat(carro.getAtivo()).isFalse(); // Verificando que o carro foi inativado no estoque

        verify(cadastroCarroService).salvar(carro);
        verify(vendaRepository).save(any(Venda.class));
    }

    @Test
    @DisplayName("Deve rejeitar venda quando o veículo estiver inativo ou já vendido")
    void deveRejeitarVendaDeVeiculoInativo() {
        Carro carroInativo = new Carro();
        carroInativo.setId(2L);
        carroInativo.setMarca("Ford");
        carroInativo.setModelo("Ka");
        carroInativo.setAtivo(false);

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setAtivo(true);

        when(cadastroCarroService.buscarOuFalhar(2L)).thenReturn(carroInativo);
        when(cadastroClienteService.buscarOuFalhar(10L)).thenReturn(cliente);

        assertThatThrownBy(() -> emissaoVendaService.realizarVenda(2L, 10L, new BigDecimal("40000.00"), null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("não está disponível para venda");

        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar venda para cliente inativo")
    void deveRejeitarVendaParaClienteInativo() {
        Carro carro = new Carro();
        carro.setId(3L);
        carro.setAtivo(true);

        Cliente clienteInativo = new Cliente();
        clienteInativo.setId(20L);
        clienteInativo.setNome("Cliente Inativo");
        clienteInativo.setAtivo(false);

        when(cadastroCarroService.buscarOuFalhar(3L)).thenReturn(carro);
        when(cadastroClienteService.buscarOuFalhar(20L)).thenReturn(clienteInativo);

        assertThatThrownBy(() -> emissaoVendaService.realizarVenda(3L, 20L, new BigDecimal("50000.00"), null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("está inativo e não pode realizar compras");

        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar venda com sucesso alterando valores e observações sem trocar carro/cliente")
    void deveAtualizarVendaComSucessoMesmoCarroECliente() {
        Carro carro = new Carro();
        carro.setId(1L);

        Cliente cliente = new Cliente();
        cliente.setId(10L);

        Venda venda = new Venda();
        venda.setId(50L);
        venda.setCarro(carro);
        venda.setCliente(cliente);
        venda.setValorVenda(new BigDecimal("100000.00"));
        venda.setObservacoes("Original");

        when(vendaRepository.findById(50L)).thenReturn(Optional.of(venda));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

        Venda atualizada = emissaoVendaService.atualizar(50L, 1L, 10L, new BigDecimal("95000.00"), "Desconto final");

        assertThat(atualizada.getValorVenda()).isEqualTo(new BigDecimal("95000.00"));
        assertThat(atualizada.getObservacoes()).isEqualTo("Desconto final");
        verify(vendaRepository).save(venda);
        verify(cadastroCarroService, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve atualizar venda trocando veículo: reativando anterior e inativando novo")
    void deveAtualizarVendaTrocandoVeiculo() {
        Carro carroAntigo = new Carro();
        carroAntigo.setId(1L);
        carroAntigo.setAtivo(false);

        Carro carroNovo = new Carro();
        carroNovo.setId(2L);
        carroNovo.setMarca("BMW");
        carroNovo.setModelo("320i");
        carroNovo.setAtivo(true);

        Cliente cliente = new Cliente();
        cliente.setId(10L);

        Venda venda = new Venda();
        venda.setId(50L);
        venda.setCarro(carroAntigo);
        venda.setCliente(cliente);
        venda.setValorVenda(new BigDecimal("100000.00"));

        when(vendaRepository.findById(50L)).thenReturn(Optional.of(venda));
        when(cadastroCarroService.buscarOuFalhar(2L)).thenReturn(carroNovo);
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

        Venda atualizada = emissaoVendaService.atualizar(50L, 2L, 10L, new BigDecimal("180000.00"), "Troca de veículo");

        assertThat(atualizada.getCarro().getId()).isEqualTo(2L);
        assertThat(carroAntigo.getAtivo()).isTrue(); // Antigo volta ao estoque
        assertThat(carroNovo.getAtivo()).isFalse();  // Novo sai do estoque

        verify(cadastroCarroService).salvar(carroAntigo);
        verify(cadastroCarroService).salvar(carroNovo);
        verify(vendaRepository).save(venda);
    }

    @Test
    @DisplayName("Deve cancelar venda e reativar o veículo no estoque")
    void deveCancelarVendaEReativarVeiculo() {
        Carro carro = new Carro();
        carro.setId(1L);
        carro.setAtivo(false);

        Venda venda = new Venda();
        venda.setId(50L);
        venda.setCarro(carro);

        when(vendaRepository.findById(50L)).thenReturn(Optional.of(venda));

        emissaoVendaService.cancelar(50L);

        assertThat(carro.getAtivo()).isTrue(); // Veículo reativado
        verify(cadastroCarroService).salvar(carro);
        verify(vendaRepository).delete(venda);
    }

    @Test
    @DisplayName("Não deve excluir venda já cancelada com auditoria nem reativar o veículo (venda dupla)")
    void naoDeveExcluirVendaJaCanceladaComAuditoria() {
        Carro carro = new Carro();
        carro.setId(1L);
        carro.setAtivo(false); // Revendido depois do cancelamento auditado

        Venda venda = new Venda();
        venda.setId(51L);
        venda.setCarro(carro);
        venda.setCancelada(true);

        when(vendaRepository.findById(51L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> emissaoVendaService.cancelar(51L))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("já foi cancelada com auditoria");

        assertThat(carro.getAtivo()).isFalse();
        verify(cadastroCarroService, never()).salvar(any());
        verify(vendaRepository, never()).delete(any(Venda.class));
    }

    /*------------- Integridade de estado (Etapa 2) ----------*/

    @Test
    @DisplayName("Deve recusar venda direta de veículo com reserva vigente de proposta")
    void deveRecusarVendaDiretaDeVeiculoReservado() {
        Carro carro = new Carro();
        carro.setId(4L);
        carro.setMarca("Honda");
        carro.setModelo("Civic");
        carro.setAtivo(true);
        carro.setDisponibilidadeComercial(DisponibilidadeComercial.RESERVADO);
        carro.setReservaValidaAte(OffsetDateTime.now().plusHours(2));
        carro.setReservaResponsavel("PC-2026-0001");

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setAtivo(true);

        when(cadastroCarroService.buscarOuFalhar(4L)).thenReturn(carro);
        when(cadastroClienteService.buscarOuFalhar(10L)).thenReturn(cliente);

        assertThatThrownBy(() -> emissaoVendaService.realizarVenda(4L, 10L, new BigDecimal("120000.00"), null))
                .isInstanceOf(ConflitoNegociacaoException.class)
                .hasMessageContaining("está reservado para a proposta PC-2026-0001");

        assertThat(carro.getAtivo()).isTrue();
        verify(cadastroCarroService, never()).salvar(any());
        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir venda direta quando a reserva do veículo já expirou")
    void devePermitirVendaDiretaComReservaExpirada() {
        Carro carro = new Carro();
        carro.setId(5L);
        carro.setAtivo(true);
        carro.setDisponibilidadeComercial(DisponibilidadeComercial.RESERVADO);
        carro.setReservaValidaAte(OffsetDateTime.now().minusMinutes(1));

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setAtivo(true);

        when(cadastroCarroService.buscarOuFalhar(5L)).thenReturn(carro);
        when(cadastroClienteService.buscarOuFalhar(10L)).thenReturn(cliente);
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

        emissaoVendaService.realizarVenda(5L, 10L, new BigDecimal("90000.00"), null);

        assertThat(carro.getDisponibilidadeComercial()).isEqualTo(DisponibilidadeComercial.VENDIDO);
        assertThat(carro.getReservaValidaAte()).isNull();
    }

    @Test
    @DisplayName("Não deve alterar venda já cancelada")
    void naoDeveAtualizarVendaCancelada() {
        Venda venda = vendaDireta(60L, carroVendido(1L));
        venda.setCancelada(true);

        when(vendaRepository.findById(60L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> emissaoVendaService.atualizar(60L, 1L, 10L, new BigDecimal("1.00"), null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Vendas canceladas não podem ser alteradas");

        assertThat(venda.getValorVenda()).isEqualByComparingTo("100000.00");
        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve alterar venda originada de proposta aprovada (valores congelados)")
    void naoDeveAtualizarVendaConvertidaDeProposta() {
        Venda venda = vendaDireta(61L, carroVendido(1L));
        venda.setRevisaoConvertida(new RevisaoProposta());
        venda.setOrigemLegada(false);

        when(vendaRepository.findById(61L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> emissaoVendaService.atualizar(61L, 1L, 10L, new BigDecimal("1.00"), null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("valores congelados");

        assertThat(venda.getValorVenda()).isEqualByComparingTo("100000.00");
        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve trocar para um veículo com reserva vigente ao atualizar a venda")
    void naoDeveTrocarParaVeiculoReservadoAoAtualizar() {
        Carro carroAtual = carroVendido(1L);
        Carro carroReservado = new Carro();
        carroReservado.setId(2L);
        carroReservado.setAtivo(true);
        carroReservado.setDisponibilidadeComercial(DisponibilidadeComercial.RESERVADO);
        carroReservado.setReservaValidaAte(OffsetDateTime.now().plusHours(1));

        Venda venda = vendaDireta(62L, carroAtual);

        when(vendaRepository.findById(62L)).thenReturn(Optional.of(venda));
        when(cadastroCarroService.buscarOuFalhar(2L)).thenReturn(carroReservado);

        assertThatThrownBy(() -> emissaoVendaService.atualizar(62L, 2L, 10L, new BigDecimal("1.00"), null))
                .isInstanceOf(ConflitoNegociacaoException.class);

        assertThat(carroAtual.getAtivo()).isFalse();
        assertThat(carroReservado.getAtivo()).isTrue();
        verify(cadastroCarroService, never()).salvar(any());
    }

    @Test
    @DisplayName("Não deve excluir venda originada de proposta: exige o cancelamento auditado")
    void naoDeveExcluirVendaConvertidaDeProposta() {
        Carro carro = carroVendido(1L);
        Venda venda = vendaDireta(63L, carro);
        venda.setRevisaoConvertida(new RevisaoProposta());

        when(vendaRepository.findById(63L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> emissaoVendaService.cancelar(63L))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Use o cancelamento auditado");

        assertThat(carro.getAtivo()).isFalse();
        verify(cadastroCarroService, never()).salvar(any());
        verify(vendaRepository, never()).delete(any(Venda.class));
    }

    @Test
    @DisplayName("Não deve reabrir no estoque um veículo que possui outra venda vigente (exclusão legada)")
    void naoDeveExcluirVendaQuandoVeiculoTemOutraVendaVigente() {
        Carro carro = carroVendido(1L);
        Venda venda = vendaDireta(64L, carro);

        when(vendaRepository.findById(64L)).thenReturn(Optional.of(venda));
        when(vendaRepository.existsByCarroIdAndCanceladaFalseAndIdNot(1L, 64L)).thenReturn(true);

        assertThatThrownBy(() -> emissaoVendaService.cancelar(64L))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("possui outra venda vigente");

        assertThat(carro.getAtivo()).isFalse();
        assertThat(carro.getDisponibilidadeComercial()).isEqualTo(DisponibilidadeComercial.VENDIDO);
        verify(cadastroCarroService, never()).salvar(any());
        verify(vendaRepository, never()).delete(any(Venda.class));
    }

    @Test
    @DisplayName("Cancelamento auditado não deve reativar veículo que possui outra venda vigente")
    void cancelamentoAuditadoNaoDeveReativarVeiculoComOutraVendaVigente() {
        Carro carro = carroVendido(1L);
        Venda venda = vendaDireta(65L, carro);

        when(vendaRepository.findById(65L)).thenReturn(Optional.of(venda));
        when(vendaRepository.existsByCarroIdAndCanceladaFalseAndIdNot(1L, 65L)).thenReturn(true);

        assertThatThrownBy(() -> emissaoVendaService.cancelarComAuditoria(65L, "Desistência", "gerente", true))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("possui outra venda vigente");

        assertThat(carro.getAtivo()).isFalse();
        assertThat(venda.getCancelada()).isFalse();
        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancelamento auditado deve registrar a trilha e devolver o veículo ao estoque")
    void cancelamentoAuditadoDeveRegistrarTrilhaEReativarVeiculo() {
        Carro carro = carroVendido(1L);
        Venda venda = vendaDireta(66L, carro);

        when(vendaRepository.findById(66L)).thenReturn(Optional.of(venda));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

        Venda cancelada = emissaoVendaService.cancelarComAuditoria(66L, "  Desistência do cliente  ", "gerente", true);

        assertThat(cancelada.getCancelada()).isTrue();
        assertThat(cancelada.getMotivoCancelamento()).isEqualTo("Desistência do cliente");
        assertThat(cancelada.getDataCancelamento()).isNotNull();
        assertThat(carro.getAtivo()).isTrue();
        assertThat(carro.getDisponibilidadeComercial()).isEqualTo(DisponibilidadeComercial.DISPONIVEL);
        verify(cadastroCarroService).salvar(carro);
        verify(vendaRepository, never()).delete(any(Venda.class));
    }

    @Test
    @DisplayName("Cancelamento auditado deve recusar motivo maior que a coluna de auditoria (255)")
    void cancelamentoAuditadoDeveRecusarMotivoLongo() {
        Carro carro = carroVendido(1L);
        Venda venda = vendaDireta(67L, carro);

        when(vendaRepository.findById(67L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> emissaoVendaService.cancelarComAuditoria(67L, "x".repeat(256), "gerente", true))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no máximo 255 caracteres");

        assertThat(carro.getAtivo()).isFalse();
        verify(cadastroCarroService, never()).salvar(any());
        verify(vendaRepository, never()).save(any());
    }

    private static Carro carroVendido(Long id) {
        Carro carro = new Carro();
        carro.setId(id);
        carro.setMarca("Toyota");
        carro.setModelo("Corolla");
        carro.marcarComoVendido();
        return carro;
    }

    private static Venda vendaDireta(Long id, Carro carro) {
        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setAtivo(true);

        Venda venda = new Venda();
        venda.setId(id);
        venda.setCarro(carro);
        venda.setCliente(cliente);
        venda.setValorVenda(new BigDecimal("100000.00"));
        return venda;
    }

    @Test
    @DisplayName("Deve lançar VendaNaoEncontradaException para ID inexistente")
    void deveLancarExcecaoAoBuscarVendaInexistente() {
        when(vendaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emissaoVendaService.buscarOuFalhar(999L))
                .isInstanceOf(VendaNaoEncontradaException.class)
                .hasMessageContaining("Não existe um registro de venda com código 999");
    }
}
