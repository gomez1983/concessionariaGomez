package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.ClienteNaoEncontradoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadastroClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private CadastroClienteService cadastroClienteService;

    @Test
    @DisplayName("Deve salvar cliente com sucesso quando CPF/CNPJ não for duplicado")
    void deveSalvarClienteComSucesso() {
        Cliente cliente = new Cliente();
        cliente.setNome("Maria Santos");
        cliente.setCpfCnpj("12345678900");

        when(clienteRepository.existsByCpfCnpj("12345678900")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        Cliente salvo = cadastroClienteService.salvar(cliente);

        assertThat(salvo).isNotNull();
        assertThat(salvo.getId()).isEqualTo(1L);
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve lançar NegocioException ao tentar salvar novo cliente com CPF/CNPJ já cadastrado")
    void deveLancarNegocioExceptionQuandoCpfDuplicado() {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Duplicado");
        cliente.setCpfCnpj("12345678900");

        when(clienteRepository.existsByCpfCnpj("12345678900")).thenReturn(true);

        assertThatThrownBy(() -> cadastroClienteService.salvar(cliente))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Já existe um cliente cadastrado com o CPF/CNPJ 12345678900");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve ativar cliente existente com sucesso")
    void deveAtivarCliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.inativar();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        cadastroClienteService.ativar(1L);

        assertThat(cliente.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve inativar cliente existente com sucesso")
    void deveInativarCliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.ativar();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        cadastroClienteService.inativar(1L);

        assertThat(cliente.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao buscar cliente inexistente")
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cadastroClienteService.buscarOuFalhar(99L))
                .isInstanceOf(ClienteNaoEncontradoException.class)
                .hasMessageContaining("Não existe um cadastro de cliente com código 99");
    }
}
