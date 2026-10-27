package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MotorFinanceiroComercialServiceTest {

    private MotorFinanceiroComercialService motorFinanceiro;
    private PoliticaComercial politicaPadrao;

    @BeforeEach
    void setUp() {
        motorFinanceiro = new MotorFinanceiroComercialService();

        politicaPadrao = new PoliticaComercial();
        politicaPadrao.setContexto(ContextoPoliticaComercial.LOJA_PADRAO);
        politicaPadrao.setVersao(1);
        politicaPadrao.setMargemDesejada(new BigDecimal("0.100000"));
        politicaPadrao.setMargemAutonomia(new BigDecimal("0.070000"));
        politicaPadrao.setMargemPisoAbsoluto(new BigDecimal("0.050000"));
        politicaPadrao.setResponsavelCadastro("gestor@concessionaria.com");
        politicaPadrao.setVigenteAPartirDe(OffsetDateTime.now().minusDays(5));
        politicaPadrao.setAtiva(true);
    }

    @Test
    @DisplayName("Cenário 1 Homologação: Aquisição R$ 90.000 e venda R$ 100.000 sem outros custos -> contribuição R$ 10.000, margem 10% e markup ~11,11%")
    void deveCalcularCenarioHomologacao1() {
        var cenario = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("100000.00"))
                .politica(politicaPadrao)
                .build();

        var resultado = motorFinanceiro.analisar(cenario);

        assertThat(resultado.getContribuicaoEstimada()).isEqualByComparingTo("10000.00");
        assertThat(resultado.getMargemContribuicaoEstimada()).isEqualByComparingTo("0.100000");
        assertThat(resultado.getMarkupAquisicao()).isEqualByComparingTo("0.111111");
        assertThat(resultado.getAlcada()).isEqualTo(AlcadaAnaliseFinanceira.DENTRO_AUTONOMIA);
    }

    @Test
    @DisplayName("Cenário 2 Homologação: Aquisição R$ 90.000 com acréscimo de 10% (R$ 99.000) -> margem sobre venda ~9,09%")
    void deveCalcularCenarioHomologacao2() {
        var cenario = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("99000.00"))
                .precoNegociado(new BigDecimal("99000.00"))
                .politica(politicaPadrao)
                .build();

        var resultado = motorFinanceiro.analisar(cenario);

        assertThat(resultado.getContribuicaoEstimada()).isEqualByComparingTo("9000.00");
        assertThat(resultado.getMargemContribuicaoEstimada()).isEqualByComparingTo("0.090909");
        assertThat(resultado.getMarkupAquisicao()).isEqualByComparingTo("0.100000");
        assertThat(resultado.getAlcada()).isEqualTo(AlcadaAnaliseFinanceira.DENTRO_AUTONOMIA);
    }

    @Test
    @DisplayName("Cenário 3 Homologação: Aquisição R$ 90.000 e margem mínima 5% -> piso R$ 94.736,85 (arredondado para cima) e bloqueio em R$ 94.736,84")
    void deveCalcularPisoArredondadoParaCimaEBloquearAproximacaoInferior() {
        var noPiso = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("94736.85"))
                .politica(politicaPadrao)
                .build();

        var resultadoNoPiso = motorFinanceiro.analisar(noPiso);

        assertThat(resultadoNoPiso.getPisoAbsoluto()).isEqualByComparingTo("94736.85");
        assertThat(resultadoNoPiso.getDescontoReais()).isEqualByComparingTo("5263.15");
        assertThat(resultadoNoPiso.getAlcada()).isEqualTo(AlcadaAnaliseFinanceira.EXIGE_APROVACAO_GERENCIAL);

        // R$ 94.736,84 gera margem ~4,99999789% -> não pode ser aprovada por arredondamento visual
        var umCentavoAbaixo = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("94736.84"))
                .politica(politicaPadrao)
                .build();

        var resultadoAbaixo = motorFinanceiro.analisar(umCentavoAbaixo);
        assertThat(resultadoAbaixo.getAlcada()).isEqualTo(AlcadaAnaliseFinanceira.BLOQUEADA_ABAIXO_PISO);
    }

    @Test
    @DisplayName("Cenário 4 Homologação: Aquisição R$ 90.000, custos fixos R$ 2.000, taxas de 3% sobre P e venda R$ 100.000 -> contribuição R$ 5.000 e margem 5%")
    void deveCalcularCenarioHomologacao4ComCustosFixosETaxasProporcionais() {
        var custoPreparacao = new ItemFinanceiroRevisao();
        custoPreparacao.setNatureza(NaturezaItemFinanceiro.PREPARACAO_RECONDICIONAMENTO);
        custoPreparacao.setBaseCalculo(BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS);
        custoPreparacao.setValor(new BigDecimal("2000.00"));
        custoPreparacao.setConfirmado(true);
        custoPreparacao.setDescricao("Preparação e polimento");

        var taxaComissao = new ItemFinanceiroRevisao();
        taxaComissao.setNatureza(NaturezaItemFinanceiro.COMISSAO_VENDEDOR);
        taxaComissao.setBaseCalculo(BaseCalculoItemFinanceiro.TAXA_SOBRE_PRECO_NEGOCIADO);
        taxaComissao.setValor(new BigDecimal("0.030000"));
        taxaComissao.setConfirmado(true);
        taxaComissao.setDescricao("Comissão e taxas proporcionais (3%)");

        var cenario = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("100000.00"))
                .politica(politicaPadrao)
                .itensFinanceiros(List.of(custoPreparacao, taxaComissao))
                .build();

        var resultado = motorFinanceiro.analisar(cenario);

        assertThat(resultado.getCustosFixosOperacao()).isEqualByComparingTo("2000.00");
        assertThat(resultado.getSomaTaxasProporcionais()).isEqualByComparingTo("0.030000");
        assertThat(resultado.getContribuicaoEstimada()).isEqualByComparingTo("5000.00");
        assertThat(resultado.getMargemContribuicaoEstimada()).isEqualByComparingTo("0.050000");
        assertThat(resultado.getPisoAbsoluto()).isEqualByComparingTo("100000.00");
        assertThat(resultado.getAlcada()).isEqualTo(AlcadaAnaliseFinanceira.EXIGE_APROVACAO_GERENCIAL);
    }

    @Test
    @DisplayName("Deve exigir aprovação gerencial quando margem está na autonomia mas o desconto em reais supera o limite da política")
    void deveAplicarRegraMaisRestritivaQuandoDescontoSuperaLimiteMesmoComMargemAlta() {
        politicaPadrao.setDescontoMaximoAutonomiaReais(new BigDecimal("2000.00"));

        // Aquisição 80.000, Anunciado 100.000, Negociado 97.000 -> Margem 17,52% (>= 7%), mas desconto é R$ 3.000 (> R$ 2.000)
        var cenario = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("80000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("97000.00"))
                .politica(politicaPadrao)
                .build();

        var resultado = motorFinanceiro.analisar(cenario);

        assertThat(resultado.getAlcada()).isEqualTo(AlcadaAnaliseFinanceira.EXIGE_APROVACAO_GERENCIAL);
        assertThat(resultado.getPisoAutonomia()).isEqualByComparingTo("98000.00");
        assertThat(resultado.getMotivosAlcada()).anyMatch(m -> m.contains("Desconto em reais supera o limite"));
    }

    @Test
    @DisplayName("Deve sinalizar pendência financeira quando houver custo desconhecido ou não confirmado, distinguindo de custo zero confirmado")
    void deveBloquearLiberacaoQuandoHouverCustoPendenteDeConfirmacao() {
        var custoZeroConfirmado = new ItemFinanceiroRevisao();
        custoZeroConfirmado.setNatureza(NaturezaItemFinanceiro.FRETE_LOJA);
        custoZeroConfirmado.setBaseCalculo(BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS);
        custoZeroConfirmado.setValor(BigDecimal.ZERO);
        custoZeroConfirmado.setConfirmado(true);
        custoZeroConfirmado.setDescricao("Frete isento confirmado");

        var cenarioConfirmado = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("100000.00"))
                .politica(politicaPadrao)
                .itensFinanceiros(List.of(custoZeroConfirmado))
                .build();

        assertThat(motorFinanceiro.analisar(cenarioConfirmado).getAlcada())
                .isEqualTo(AlcadaAnaliseFinanceira.DENTRO_AUTONOMIA);

        var custoDesconhecido = new ItemFinanceiroRevisao();
        custoDesconhecido.setNatureza(NaturezaItemFinanceiro.PREPARACAO_RECONDICIONAMENTO);
        custoDesconhecido.setBaseCalculo(BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS);
        custoDesconhecido.setValor(null);
        custoDesconhecido.setConfirmado(false);
        custoDesconhecido.setDescricao("Orçamento de funilaria pendente");

        var cenarioPendente = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("100000.00"))
                .politica(politicaPadrao)
                .itensFinanceiros(List.of(custoDesconhecido))
                .build();

        var resultadoPendente = motorFinanceiro.analisar(cenarioPendente);
        assertThat(resultadoPendente.isPossuiCustosPendentes()).isTrue();
        assertThat(resultadoPendente.getAlcada()).isEqualTo(AlcadaAnaliseFinanceira.PENDENTE_DADOS_OBRIGATORIOS);
    }

    @Test
    @DisplayName("Deve rejeitar cenário com denominador inválido (1 - t - m <= 0) e políticas com invariantes violadas")
    void deveRejeitarDenominadorInvalidoEInvarianteDePoliticaViolada() {
        var taxaExcessiva = new ItemFinanceiroRevisao();
        taxaExcessiva.setNatureza(NaturezaItemFinanceiro.IMPOSTO_OPERACAO);
        taxaExcessiva.setBaseCalculo(BaseCalculoItemFinanceiro.TAXA_SOBRE_PRECO_NEGOCIADO);
        taxaExcessiva.setValor(new BigDecimal("0.920000"));
        taxaExcessiva.setConfirmado(true);
        taxaExcessiva.setDescricao("Taxa inviável");

        var cenarioInviavel = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("100000.00"))
                .politica(politicaPadrao)
                .itensFinanceiros(List.of(taxaExcessiva))
                .build();

        assertThatThrownBy(() -> motorFinanceiro.analisar(cenarioInviavel))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("1 - t - m <= 0");

        PoliticaComercial politicaInvalida = new PoliticaComercial();
        politicaInvalida.setContexto(ContextoPoliticaComercial.LOJA_PADRAO);
        politicaInvalida.setMargemDesejada(new BigDecimal("0.05"));
        politicaInvalida.setMargemAutonomia(new BigDecimal("0.08")); // maior que desejada!
        politicaInvalida.setMargemPisoAbsoluto(new BigDecimal("0.04"));

        assertThatThrownBy(politicaInvalida::validarInvariantes)
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("margemDesejada >= margemAutonomia >= margemPisoAbsoluto");
    }

    @Test
    @DisplayName("Deve dar precedência à política de exceção por veículo sobre a política padrão da loja")
    void deveResolverPoliticaComPrecedenciaDeExcecaoPorVeiculo() {
        OffsetDateTime agora = OffsetDateTime.now();
        Carro carro = new Carro();
        carro.setId(42L);

        PoliticaComercial excecaoVeiculo = new PoliticaComercial();
        excecaoVeiculo.setContexto(ContextoPoliticaComercial.EXCECAO_VEICULO);
        excecaoVeiculo.setCarro(carro);
        excecaoVeiculo.setVersao(2);
        excecaoVeiculo.setMargemDesejada(new BigDecimal("0.080000"));
        excecaoVeiculo.setMargemAutonomia(new BigDecimal("0.050000"));
        excecaoVeiculo.setMargemPisoAbsoluto(new BigDecimal("0.030000"));
        excecaoVeiculo.setResponsavelCadastro("diretor@concessionaria.com");
        excecaoVeiculo.setVigenteAPartirDe(agora.minusDays(1));
        excecaoVeiculo.setAtiva(true);

        PoliticaComercial resolvida = motorFinanceiro.resolverPoliticaVigente(
                carro, List.of(politicaPadrao, excecaoVeiculo), agora);

        assertThat(resolvida.getContexto()).isEqualTo(ContextoPoliticaComercial.EXCECAO_VEICULO);
        assertThat(resolvida.getMargemPisoAbsoluto()).isEqualByComparingTo("0.030000");
    }

    @Test
    @DisplayName("Deve ignorar benefício já refletido no preço/custo para evitar dupla contagem em B e validar avaliação de troca")
    void deveEvitarDuplaContagemDeCampanhaECalcularCreditoLiquidoDeTroca() {
        var campanhaJaRefletida = new CampanhaComercial();
        campanhaJaRefletida.setCodigo("BONUS-JA-APLICADO");
        campanhaJaRefletida.setValorBeneficio(new BigDecimal("3000.00"));
        campanhaJaRefletida.setReembolsavelMontadora(true);
        campanhaJaRefletida.setJaRefletidaNoPrecoOuCusto(true);
        campanhaJaRefletida.setCumulativa(true);
        campanhaJaRefletida.setAtiva(true);

        var cenario = MotorFinanceiroComercialService.CenarioFinanceiroInput.builder()
                .custoAquisicao(new BigDecimal("90000.00"))
                .precoAnunciado(new BigDecimal("100000.00"))
                .precoNegociado(new BigDecimal("100000.00"))
                .politica(politicaPadrao)
                .campanhasAplicadas(List.of(campanhaJaRefletida))
                .build();

        var resultado = motorFinanceiro.analisar(cenario);
        assertThat(resultado.getReceitasAdicionaisElegiveis()).isEqualByComparingTo("0.00");

        // Avaliação de troca: aceito por R$ 40.000 com quitação de R$ 10.000 -> crédito líquido R$ 30.000
        var troca = new AvaliacaoTroca();
        troca.setIdentificacaoVeiculoUsado("VW Polo 2020 ABC-1234");
        troca.setValorAvaliacaoTecnica(new BigDecimal("38000.00"));
        troca.setValorComercialAceito(new BigDecimal("40000.00"));
        troca.setSaldoQuitacaoVerificado(new BigDecimal("10000.00"));

        assertThat(troca.calcularCreditoLiquido()).isEqualByComparingTo("30000.00");
        assertThat(troca.calcularSuperavaliacaoSuportadaPelaLoja()).isEqualByComparingTo("2000.00");
        assertThat(troca.possuiSaldoDevedorSuperiorAvaliacao()).isFalse();
    }
}
