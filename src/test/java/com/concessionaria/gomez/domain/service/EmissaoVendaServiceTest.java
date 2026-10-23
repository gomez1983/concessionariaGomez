package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.exception.VendaNaoEncontradaException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.model.Venda;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    @DisplayName("Deve lançar VendaNaoEncontradaException para ID inexistente")
    void deveLancarExcecaoAoBuscarVendaInexistente() {
        when(vendaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emissaoVendaService.buscarOuFalhar(999L))
                .isInstanceOf(VendaNaoEncontradaException.class)
                .hasMessageContaining("Não existe um registro de venda com código 999");
    }
}
