package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.DisponibilidadeComercial;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastroCarroServiceTest {

    @Mock
    private CarroRepository carroRepository;

    @Mock
    private VendaRepository vendaRepository;

    @InjectMocks
    private CadastroCarroService cadastroCarroService;

    @Test
    @DisplayName("Não deve reativar pelo atalho de estoque um veículo com venda vigente")
    void naoDeveAtivarVeiculoComVendaVigente() {
        Carro carro = new Carro();
        carro.setId(1L);
        carro.marcarComoVendido();

        when(carroRepository.findById(1L)).thenReturn(Optional.of(carro));
        when(vendaRepository.existsByCarroIdAndCanceladaFalse(1L)).thenReturn(true);

        assertThatThrownBy(() -> cadastroCarroService.ativar(1L))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("possui uma venda vigente");

        assertThat(carro.getAtivo()).isFalse();
        assertThat(carro.getDisponibilidadeComercial()).isEqualTo(DisponibilidadeComercial.VENDIDO);
    }

    @Test
    @DisplayName("Não deve reativar veículo com venda vigente mesmo com disponibilidade inconsistente (dado legado)")
    void naoDeveAtivarVeiculoComVendaVigenteEDisponibilidadeInconsistente() {
        Carro carro = new Carro();
        carro.setId(2L);
        carro.setAtivo(false); // DISPONIVEL por padrão, mas com venda vigente

        when(carroRepository.findById(2L)).thenReturn(Optional.of(carro));
        when(vendaRepository.existsByCarroIdAndCanceladaFalse(2L)).thenReturn(true);

        assertThatThrownBy(() -> cadastroCarroService.ativar(2L))
                .isInstanceOf(NegocioException.class);

        assertThat(carro.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve reativar veículo vendido cuja venda foi cancelada sem devolução ao estoque")
    void deveAtivarVeiculoVendidoSemVendaVigente() {
        Carro carro = new Carro();
        carro.setId(3L);
        carro.marcarComoVendido();

        when(carroRepository.findById(3L)).thenReturn(Optional.of(carro));
        when(vendaRepository.existsByCarroIdAndCanceladaFalse(3L)).thenReturn(false);

        cadastroCarroService.ativar(3L);

        assertThat(carro.getAtivo()).isTrue();
        assertThat(carro.getDisponibilidadeComercial()).isEqualTo(DisponibilidadeComercial.DISPONIVEL);
    }

    @Test
    @DisplayName("Deve reativar veículo inativado manualmente (sem venda)")
    void deveAtivarVeiculoInativadoManualmente() {
        Carro carro = new Carro();
        carro.setId(4L);
        carro.inativar();

        when(carroRepository.findById(4L)).thenReturn(Optional.of(carro));

        cadastroCarroService.ativar(4L);

        assertThat(carro.getAtivo()).isTrue();
        assertThat(carro.getDisponibilidadeComercial()).isEqualTo(DisponibilidadeComercial.DISPONIVEL);
    }
}
