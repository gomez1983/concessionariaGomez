package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.AcessoComercialNegadoException;
import com.concessionaria.gomez.domain.exception.ConflitoNegociacaoException;
import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.AvaliacaoTroca;
import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.model.Cliente;
import com.concessionaria.gomez.domain.model.ContextoPoliticaComercial;
import com.concessionaria.gomez.domain.model.DisponibilidadeComercial;
import com.concessionaria.gomez.domain.model.ModalidadePagamentoProposta;
import com.concessionaria.gomez.domain.model.PagamentoProposta;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.model.PoliticaComercial;
import com.concessionaria.gomez.domain.model.PropostaComercial;
import com.concessionaria.gomez.domain.model.RevisaoProposta;
import com.concessionaria.gomez.domain.model.StatusPropostaComercial;
import com.concessionaria.gomez.domain.model.Venda;
import com.concessionaria.gomez.domain.repository.CampanhaComercialRepository;
import com.concessionaria.gomez.domain.repository.CustoVeiculoRepository;
import com.concessionaria.gomez.domain.repository.PoliticaComercialRepository;
import com.concessionaria.gomez.domain.repository.PropostaComercialRepository;
import com.concessionaria.gomez.domain.repository.RevisaoPropostaRepository;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import com.concessionaria.gomez.domain.service.GestaoPropostaComercialService.DadosRevisaoCommand;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestaoPropostaComercialServiceTest {

    @Mock
    private PropostaComercialRepository propostaRepository;

    @Mock
    private RevisaoPropostaRepository revisaoRepository;

    @Mock
    private PoliticaComercialRepository politicaRepository;

    @Mock
    private CustoVeiculoRepository custoVeiculoRepository;

    @Mock
    private CampanhaComercialRepository campanhaRepository;

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private CadastroCarroService cadastroCarroService;

    @Mock
    private CadastroClienteService cadastroClienteService;

    private GestaoPropostaComercialService service;
    private Carro carroPadrao;
    private Cliente clientePadrao;
    private PoliticaComercial politicaPadrao;

    @BeforeEach
    void setUp() {
        MotorFinanceiroComercialService motorFinanceiro = new MotorFinanceiroComercialService();
        service = new GestaoPropostaComercialService(
                propostaRepository,
                revisaoRepository,
                politicaRepository,
                custoVeiculoRepository,
                campanhaRepository,
                vendaRepository,
                cadastroCarroService,
                cadastroClienteService,
                motorFinanceiro
        );

        // Compra = 100.000 -> Preço anunciado efetivo (sugerido 15% markup) = 115.000,00
        carroPadrao = new Carro();
        carroPadrao.setId(10L);
        carroPadrao.setMarca("Toyota");
        carroPadrao.setModelo("Corolla XEi");
        carroPadrao.setAno(2023);
        carroPadrao.setCompra(new BigDecimal("100000.00"));
        carroPadrao.setQuilometragem(15000);
        carroPadrao.setAtivo(true);
        carroPadrao.setDisponibilidadeComercial(DisponibilidadeComercial.DISPONIVEL);

        clientePadrao = new Cliente();
        clientePadrao.setId(20L);
        clientePadrao.setNome("Maria Oliveira");
        clientePadrao.setCpfCnpj("12345678909");
        clientePadrao.setAtivo(true);

        politicaPadrao = new PoliticaComercial();
        politicaPadrao.setId(1L);
        politicaPadrao.setVersao(1);
        politicaPadrao.setContexto(ContextoPoliticaComercial.LOJA_PADRAO);
        politicaPadrao.setMargemDesejada(new BigDecimal("0.100000"));
        politicaPadrao.setMargemAutonomia(new BigDecimal("0.070000"));
        politicaPadrao.setMargemPisoAbsoluto(new BigDecimal("0.050000"));
        politicaPadrao.setDescontoMaximoAutonomiaReais(new BigDecimal("5000.00"));
        politicaPadrao.setDescontoMaximoAutonomiaPercentual(new BigDecimal("0.050000"));
        politicaPadrao.setVigenteAPartirDe(OffsetDateTime.now().minusDays(30));
        politicaPadrao.setAtiva(true);

        lenient().when(politicaRepository.findVigentes(eq(ContextoPoliticaComercial.EXCECAO_VEICULO), any(), any()))
                .thenReturn(List.of());
        lenient().when(politicaRepository.findVigentes(eq(ContextoPoliticaComercial.LOJA_PADRAO), any(), any()))
                .thenReturn(List.of(politicaPadrao));
        lenient().when(custoVeiculoRepository.findByCarroIdOrderByIdAsc(any()))
                .thenReturn(List.of());
        lenient().when(cadastroCarroService.buscarOuFalhar(10L)).thenReturn(carroPadrao);
        lenient().when(cadastroClienteService.buscarOuFalhar(20L)).thenReturn(clientePadrao);
        lenient().when(revisaoRepository.findByPropostaIdAndSequencia(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(propostaRepository.save(any(PropostaComercial.class))).thenAnswer(inv -> {
            PropostaComercial p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(100L);
            }
            for (int i = 0; i < p.getRevisoes().size(); i++) {
                if (p.getRevisoes().get(i).getId() == null) {
                    p.getRevisoes().get(i).setId((long) (500 + i));
                }
            }
            return p;
        });
    }

    @Test
    @DisplayName("Deve aprovar automaticamente proposta submetida dentro da alçada autônoma do vendedor")
    void deveAprovarAutomaticamenteDentroDaAlcadaDoVendedor() {
        // Preço anunciado = 115.000,00; Preço negociado = 112.000,00 (Desconto R$ 3.000 <= R$ 5.000; Margem = 12.000/112.000 = 10,71% >= 7%)
        PagamentoProposta pgto = new PagamentoProposta();
        pgto.setModalidade(ModalidadePagamentoProposta.PIX_TRANSFERENCIA);
        pgto.setValor(new BigDecimal("112000.00"));

        DadosRevisaoCommand command = DadosRevisaoCommand.builder()
                .carroId(10L)
                .clienteId(20L)
                .vendedorIdentificador("vendedor.joao")
                .precoNegociado(new BigDecimal("112000.00"))
                .justificativaVendedor("Cliente fiel à vista")
                .submeter(true)
                .pagamentos(List.of(pgto))
                .build();

        PropostaComercial proposta = service.criarProposta(command);

        assertThat(proposta.getStatus()).isEqualTo(StatusPropostaComercial.APROVADA);
        assertThat(proposta.getRevisoes()).hasSize(1);
        RevisaoProposta revisao = proposta.getRevisoes().get(0);
        assertThat(revisao.getSequencia()).isEqualTo(1);
        assertThat(revisao.getPrecoNegociado()).isEqualByComparingTo("112000.00");
        assertThat(revisao.getDecisoes()).hasSize(1);
    }

    @Test
    @DisplayName("Deve exigir aprovação gerencial e bloquear autoaprovação quando desconto excede alçada do vendedor")
    void deveExigirAprovacaoGerencialEBloquearAutoaprovacao() {
        // Preço negociado = 107.000,00 (Desconto R$ 8.000 > R$ 5.000, mas Margem = 7.000/107.000 = 6,54% >= 5% piso absoluto)
        PagamentoProposta pgto = new PagamentoProposta();
        pgto.setModalidade(ModalidadePagamentoProposta.PIX_TRANSFERENCIA);
        pgto.setValor(new BigDecimal("107000.00"));

        DadosRevisaoCommand command = DadosRevisaoCommand.builder()
                .carroId(10L)
                .clienteId(20L)
                .vendedorIdentificador("vendedor.joao")
                .precoNegociado(new BigDecimal("107000.00"))
                .justificativaVendedor("Negociação competitiva")
                .submeter(true)
                .pagamentos(List.of(pgto))
                .build();

        PropostaComercial proposta = service.criarProposta(command);
        when(propostaRepository.findById(100L)).thenReturn(Optional.of(proposta));

        assertThat(proposta.getStatus()).isEqualTo(StatusPropostaComercial.PENDENTE_APROVACAO);

        // Vendedor não tem permissão para decidir aprovação (403)
        assertThatThrownBy(() -> service.decidirAprovacaoGerencial(
                100L, 1, "gerente.outro", PerfilOperadorComercial.VENDEDOR, true, "Tentando aprovar"))
                .isInstanceOf(AcessoComercialNegadoException.class);

        // Autor da proposta não pode autoaprovar mesmo com perfil GERENTE (403)
        assertThatThrownBy(() -> service.decidirAprovacaoGerencial(
                100L, 1, "vendedor.joao", PerfilOperadorComercial.GERENTE, true, "Autoaprovação"))
                .isInstanceOf(AcessoComercialNegadoException.class)
                .hasMessageContaining("Conflito de interesse");

        // Revisão divergente gera conflito 409
        assertThatThrownBy(() -> service.decidirAprovacaoGerencial(
                100L, 99, "gerente.carlos", PerfilOperadorComercial.GERENTE, true, "Revisão antiga"))
                .isInstanceOf(ConflitoNegociacaoException.class);

        // Gerente distinto aprova com sucesso
        PropostaComercial aprovada = service.decidirAprovacaoGerencial(
                100L, 1, "gerente.carlos", PerfilOperadorComercial.GERENTE, true, "Exceção comercial autorizada");
        assertThat(aprovada.getStatus()).isEqualTo(StatusPropostaComercial.APROVADA);
    }

    @Test
    @DisplayName("Deve calcular crédito líquido de veículo na troca e converter proposta em venda de forma idempotente")
    void deveCalcularTrocaEConverterEmVendaComIdempotencia() {
        // Preço negociado = 115.000,00 (sem desconto).
        // Troca avaliada em R$ 40.000,00, quitação R$ 10.000,00 -> Crédito líquido = R$ 30.000,00.
        // Saldo de R$ 85.000,00 via PIX -> Total pago = R$ 115.000,00.
        AvaliacaoTroca troca = new AvaliacaoTroca();
        troca.setIdentificacaoVeiculoUsado("Honda Fit EX 2018");
        troca.setValorAvaliacaoTecnica(new BigDecimal("40000.00"));
        troca.setValorComercialAceito(new BigDecimal("40000.00"));
        troca.setSaldoQuitacaoVerificado(new BigDecimal("10000.00"));
        troca.setPossuiPendenciaDocumental(false);

        PagamentoProposta pgtoTroca = new PagamentoProposta();
        pgtoTroca.setModalidade(ModalidadePagamentoProposta.CREDITO_LIQUIDO_TROCA);
        pgtoTroca.setValor(new BigDecimal("30000.00"));

        PagamentoProposta pgtoPix = new PagamentoProposta();
        pgtoPix.setModalidade(ModalidadePagamentoProposta.PIX_TRANSFERENCIA);
        pgtoPix.setValor(new BigDecimal("85000.00"));

        DadosRevisaoCommand command = DadosRevisaoCommand.builder()
                .carroId(10L)
                .clienteId(20L)
                .vendedorIdentificador("vendedor.maria")
                .precoNegociado(new BigDecimal("115000.00"))
                .submeter(true)
                .avaliacaoTroca(troca)
                .pagamentos(List.of(pgtoTroca, pgtoPix))
                .build();

        PropostaComercial proposta = service.criarProposta(command);
        when(propostaRepository.findById(100L)).thenReturn(Optional.of(proposta));

        assertThat(proposta.getStatus()).isEqualTo(StatusPropostaComercial.APROVADA);
        assertThat(proposta.getRevisoes().get(0).getAvaliacaoTroca().getCreditoLiquido())
                .isEqualByComparingTo("30000.00");

        when(vendaRepository.findByRevisaoConvertidaId(eq(500L))).thenReturn(Optional.empty());
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            v.setId(900L);
            return v;
        });

        Venda vendaCriada = service.converterEmVenda(100L, 1, "Observação de faturamento");

        assertThat(vendaCriada.getId()).isEqualTo(900L);
        assertThat(vendaCriada.getValorVenda()).isEqualByComparingTo("115000.00");
        assertThat(proposta.getStatus()).isEqualTo(StatusPropostaComercial.CONVERTIDA);
        assertThat(carroPadrao.getAtivo()).isFalse();
        assertThat(carroPadrao.getDisponibilidadeComercial()).isEqualTo(DisponibilidadeComercial.VENDIDO);

        // Segunda chamada para a mesma revisão retorna a mesma venda de forma idempotente
        when(vendaRepository.findByRevisaoConvertidaId(eq(500L))).thenReturn(Optional.of(vendaCriada));
        Venda vendaIdempotente = service.converterEmVenda(100L, 1, "Reenvio acidental");
        assertThat(vendaIdempotente.getId()).isEqualTo(900L);
    }

    @Test
    @DisplayName("Deve impedir submissão quando composição de pagamento não conciliar com preço negociado")
    void deveImpedirSubmissaoSemConciliacaoDePagamento() {
        PagamentoProposta pgtoIncompleto = new PagamentoProposta();
        pgtoIncompleto.setModalidade(ModalidadePagamentoProposta.PIX_TRANSFERENCIA);
        pgtoIncompleto.setValor(new BigDecimal("100000.00"));

        DadosRevisaoCommand command = DadosRevisaoCommand.builder()
                .carroId(10L)
                .clienteId(20L)
                .vendedorIdentificador("vendedor.maria")
                .precoNegociado(new BigDecimal("115000.00"))
                .submeter(true)
                .pagamentos(List.of(pgtoIncompleto))
                .build();

        assertThatThrownBy(() -> service.criarProposta(command))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("composição de pagamentos");
    }
}
