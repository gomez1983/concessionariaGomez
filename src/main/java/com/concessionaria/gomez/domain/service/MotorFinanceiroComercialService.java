package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.exception.NegocioException;
import com.concessionaria.gomez.domain.model.*;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MotorFinanceiroComercialService {

    private static final MathContext PRECISAO_INTERMEDIARIA = new MathContext(16, RoundingMode.HALF_UP);
    private static final int ESCALA_MOEDA = 2;
    private static final int ESCALA_TAXA = 6;

    @Getter
    @Builder
    public static class CenarioFinanceiroInput {
        private final BigDecimal custoAquisicao;
        private final BigDecimal precoAnunciado;
        private final BigDecimal precoNegociado;
        private final PoliticaComercial politica;
        @Builder.Default
        private final List<ItemFinanceiroRevisao> itensFinanceiros = Collections.emptyList();
        @Builder.Default
        private final List<CampanhaComercial> campanhasAplicadas = Collections.emptyList();
    }

    @Getter
    @Builder
    public static class ResultadoAnaliseFinanceira {
        private final BigDecimal precoAnunciado;
        private final BigDecimal precoNegociado;
        private final BigDecimal custoAquisicao;
        private final BigDecimal custosFixosOperacao;
        private final BigDecimal somaTaxasProporcionais;
        private final BigDecimal receitasAdicionaisElegiveis;
        private final BigDecimal contribuicaoEstimada;
        private final BigDecimal margemContribuicaoEstimada;
        private final BigDecimal descontoReais;
        private final BigDecimal descontoPercentual;
        private final BigDecimal markupAquisicao;
        private final BigDecimal pisoDesejado;
        private final BigDecimal pisoAutonomia;
        private final BigDecimal pisoAbsoluto;
        private final AlcadaAnaliseFinanceira alcada;
        private final boolean possuiCustosPendentes;
        private final List<String> motivosAlcada;
    }

    /**
     * Resolve a política comercial aplicável com precedência explícita:
     * 1. Exceção por veículo vigente (maior versão).
     * 2. Política padrão da loja vigente (maior versão).
     */
    public PoliticaComercial resolverPoliticaVigente(
            Carro carro,
            List<PoliticaComercial> politicasCandidatas,
            OffsetDateTime instanteReferencia) {

        if (politicasCandidatas == null || politicasCandidatas.isEmpty()) {
            throw new NegocioException("Não há política comercial cadastrada para avaliação da proposta.");
        }
        OffsetDateTime ref = instanteReferencia != null ? instanteReferencia : OffsetDateTime.now();

        if (carro != null && carro.getId() != null) {
            var excecaoVeiculo = politicasCandidatas.stream()
                    .filter(p -> p.getContexto() == ContextoPoliticaComercial.EXCECAO_VEICULO)
                    .filter(p -> p.getCarro() != null && carro.getId().equals(p.getCarro().getId()))
                    .filter(p -> p.estaVigenteEm(ref))
                    .max((a, b) -> Integer.compare(
                            a.getVersao() != null ? a.getVersao() : 0,
                            b.getVersao() != null ? b.getVersao() : 0));

            if (excecaoVeiculo.isPresent()) {
                excecaoVeiculo.get().validarInvariantes();
                return excecaoVeiculo.get();
            }
        }

        var padraoLoja = politicasCandidatas.stream()
                .filter(p -> p.getContexto() == ContextoPoliticaComercial.LOJA_PADRAO)
                .filter(p -> p.estaVigenteEm(ref))
                .max((a, b) -> Integer.compare(
                        a.getVersao() != null ? a.getVersao() : 0,
                        b.getVersao() != null ? b.getVersao() : 0));

        PoliticaComercial escolhida = padraoLoja.orElseThrow(() ->
                new NegocioException("Não foi encontrada política comercial vigente para a operação."));
        escolhida.validarInvariantes();
        return escolhida;
    }

    /**
     * Calcula o piso matemático em centavos arredondado para cima (RoundingMode.CEILING):
     * P_min = (C + F - B) / (1 - t - m)
     */
    public BigDecimal calcularPisoMatematico(
            BigDecimal custoAquisicao,
            BigDecimal custosFixos,
            BigDecimal receitasAdicionais,
            BigDecimal somaTaxasProporcionais,
            BigDecimal metaMargem) {

        BigDecimal c = validarNaoNegativo(custoAquisicao, "Custo de aquisição");
        BigDecimal f = custosFixos != null ? validarNaoNegativo(custosFixos, "Custos fixos da operação") : BigDecimal.ZERO;
        BigDecimal b = receitasAdicionais != null ? validarNaoNegativo(receitasAdicionais, "Receitas adicionais") : BigDecimal.ZERO;
        BigDecimal t = somaTaxasProporcionais != null ? validarNaoNegativo(somaTaxasProporcionais, "Taxas proporcionais") : BigDecimal.ZERO;
        BigDecimal m = validarNaoNegativo(metaMargem, "Meta de margem");

        BigDecimal denominador = BigDecimal.ONE.subtract(t).subtract(m);
        if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Cenário financeiro inviável: a soma das taxas proporcionais e da margem meta iguala ou supera 100% (1 - t - m <= 0).");
        }

        BigDecimal numerador = c.add(f).subtract(b);
        if (numerador.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(ESCALA_MOEDA, RoundingMode.CEILING);
        }

        return numerador.divide(denominador, ESCALA_MOEDA, RoundingMode.CEILING);
    }

    public ResultadoAnaliseFinanceira analisar(CenarioFinanceiroInput input) {
        if (input == null) {
            throw new NegocioException("Os dados do cenário financeiro são obrigatórios.");
        }
        PoliticaComercial politica = input.getPolitica();
        if (politica == null) {
            throw new NegocioException("A política comercial é obrigatória para análise financeira.");
        }
        politica.validarInvariantes();

        BigDecimal c = validarPositivo(input.getCustoAquisicao(), "Custo de aquisição");
        BigDecimal p = validarPositivo(input.getPrecoNegociado(), "Preço negociado");
        BigDecimal anunciado = input.getPrecoAnunciado() != null
                ? validarPositivo(input.getPrecoAnunciado(), "Preço anunciado")
                : p;

        BigDecimal f = BigDecimal.ZERO.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);
        BigDecimal t = BigDecimal.ZERO.setScale(ESCALA_TAXA, RoundingMode.HALF_UP);
        BigDecimal b = BigDecimal.ZERO.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);
        boolean possuiPendentes = false;
        List<String> motivos = new ArrayList<>();

        if (input.getItensFinanceiros() != null) {
            for (ItemFinanceiroRevisao item : input.getItensFinanceiros()) {
                if (item.isPendenteConfirmacao()) {
                    possuiPendentes = true;
                    motivos.add("Item financeiro pendente de confirmação: " + item.getDescricao());
                    continue;
                }
                BigDecimal valorItem = validarNaoNegativo(item.getValor(), "Valor do item financeiro");
                if (item.getBaseCalculo() == BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS) {
                    f = f.add(valorItem.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP));
                } else if (item.getBaseCalculo() == BaseCalculoItemFinanceiro.TAXA_SOBRE_PRECO_NEGOCIADO) {
                    t = t.add(valorItem.setScale(ESCALA_TAXA, RoundingMode.HALF_UP));
                } else if (item.getBaseCalculo() == BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL) {
                    b = b.add(valorItem.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP));
                }
            }
        }

        if (input.getCampanhasAplicadas() != null) {
            long naoCumulativas = input.getCampanhasAplicadas().stream()
                    .filter(camp -> !Boolean.TRUE.equals(camp.getCumulativa()))
                    .count();
            if (naoCumulativas > 1 || (naoCumulativas == 1 && input.getCampanhasAplicadas().size() > 1)) {
                throw new NegocioException("Foram selecionadas campanhas comerciais não cumulativas entre si.");
            }
            for (CampanhaComercial campanha : input.getCampanhasAplicadas()) {
                if (campanha.isElegivelComoReceitaAdicional()) {
                    b = b.add(campanha.getValorBeneficio().setScale(ESCALA_MOEDA, RoundingMode.HALF_UP));
                }
            }
        }

        BigDecimal denominadorDesejado = BigDecimal.ONE.subtract(t).subtract(politica.getMargemDesejada());
        BigDecimal denominadorAutonomia = BigDecimal.ONE.subtract(t).subtract(politica.getMargemAutonomia());
        BigDecimal denominadorPiso = BigDecimal.ONE.subtract(t).subtract(politica.getMargemPisoAbsoluto());

        if (denominadorDesejado.compareTo(BigDecimal.ZERO) <= 0
                || denominadorAutonomia.compareTo(BigDecimal.ZERO) <= 0
                || denominadorPiso.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Cenário financeiro inviável: a soma das taxas proporcionais e da margem meta iguala ou supera 100% (1 - t - m <= 0).");
        }

        BigDecimal pisoDesejado = calcularPisoMatematico(c, f, b, t, politica.getMargemDesejada());
        BigDecimal pisoAutonomiaMatematico = calcularPisoMatematico(c, f, b, t, politica.getMargemAutonomia());
        BigDecimal pisoAbsoluto = calcularPisoMatematico(c, f, b, t, politica.getMargemPisoAbsoluto());

        // Aplica regra mais restritiva entre o piso matemático de autonomia e os limites de desconto sobre o anunciado
        BigDecimal pisoAutonomiaEfetivo = pisoAutonomiaMatematico;
        if (politica.getDescontoMaximoAutonomiaReais() != null) {
            BigDecimal pisoPorDescontoReais = anunciado
                    .subtract(politica.getDescontoMaximoAutonomiaReais())
                    .setScale(ESCALA_MOEDA, RoundingMode.CEILING);
            if (pisoPorDescontoReais.compareTo(pisoAutonomiaEfetivo) > 0) {
                pisoAutonomiaEfetivo = pisoPorDescontoReais;
            }
        }
        if (politica.getDescontoMaximoAutonomiaPercentual() != null) {
            BigDecimal fatorMinimo = BigDecimal.ONE.subtract(politica.getDescontoMaximoAutonomiaPercentual());
            BigDecimal pisoPorDescontoPercentual = anunciado
                    .multiply(fatorMinimo, PRECISAO_INTERMEDIARIA)
                    .setScale(ESCALA_MOEDA, RoundingMode.CEILING);
            if (pisoPorDescontoPercentual.compareTo(pisoAutonomiaEfetivo) > 0) {
                pisoAutonomiaEfetivo = pisoPorDescontoPercentual;
            }
        }

        // Cálculo de contribuição com precisão intermediária e arredondamento monetário final
        BigDecimal custoProporcional = p.multiply(t, PRECISAO_INTERMEDIARIA).setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);
        BigDecimal contribuicao = p.subtract(c).subtract(f).subtract(custoProporcional).add(b)
                .setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);

        // Margem interna exata para decisão (sem arredondar para exibição percentual antes de comparar!)
        BigDecimal margemExata = contribuicao.divide(p, PRECISAO_INTERMEDIARIA);
        BigDecimal margemEscalada = margemExata.setScale(ESCALA_TAXA, RoundingMode.HALF_UP);

        BigDecimal descontoReais = anunciado.subtract(p).max(BigDecimal.ZERO.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP))
                .setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);
        BigDecimal descontoPercentual = descontoReais.divide(anunciado, ESCALA_TAXA, RoundingMode.HALF_UP);
        BigDecimal markupAquisicao = p.subtract(c).divide(c, ESCALA_TAXA, RoundingMode.HALF_UP);

        AlcadaAnaliseFinanceira alcada;
        if (possuiPendentes) {
            alcada = AlcadaAnaliseFinanceira.PENDENTE_DADOS_OBRIGATORIOS;
        } else {
            boolean abaixoPisoMargem = margemExata.compareTo(politica.getMargemPisoAbsoluto()) < 0
                    || p.compareTo(pisoAbsoluto) < 0;

            boolean abaixoAutonomiaMargem = margemExata.compareTo(politica.getMargemAutonomia()) < 0
                    || p.compareTo(pisoAutonomiaMatematico) < 0;

            boolean excedeDescontoReais = politica.getDescontoMaximoAutonomiaReais() != null
                    && descontoReais.compareTo(politica.getDescontoMaximoAutonomiaReais()) > 0;

            BigDecimal descontoPercentualExato = descontoReais.divide(anunciado, PRECISAO_INTERMEDIARIA);
            boolean excedeDescontoPercentual = politica.getDescontoMaximoAutonomiaPercentual() != null
                    && descontoPercentualExato.compareTo(politica.getDescontoMaximoAutonomiaPercentual()) > 0;

            if (abaixoPisoMargem) {
                alcada = AlcadaAnaliseFinanceira.BLOQUEADA_ABAIXO_PISO;
                motivos.add("Preço negociado ou margem de contribuição abaixo do piso absoluto da operação.");
            } else if (abaixoAutonomiaMargem || excedeDescontoReais || excedeDescontoPercentual) {
                alcada = AlcadaAnaliseFinanceira.EXIGE_APROVACAO_GERENCIAL;
                if (abaixoAutonomiaMargem) {
                    motivos.add("Margem de contribuição abaixo da autonomia do vendedor.");
                }
                if (excedeDescontoReais) {
                    motivos.add("Desconto em reais supera o limite de autonomia do vendedor.");
                }
                if (excedeDescontoPercentual) {
                    motivos.add("Desconto percentual supera o limite de autonomia do vendedor.");
                }
            } else {
                alcada = AlcadaAnaliseFinanceira.DENTRO_AUTONOMIA;
            }
        }

        return ResultadoAnaliseFinanceira.builder()
                .precoAnunciado(anunciado.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP))
                .precoNegociado(p.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP))
                .custoAquisicao(c.setScale(ESCALA_MOEDA, RoundingMode.HALF_UP))
                .custosFixosOperacao(f)
                .somaTaxasProporcionais(t)
                .receitasAdicionaisElegiveis(b)
                .contribuicaoEstimada(contribuicao)
                .margemContribuicaoEstimada(margemEscalada)
                .descontoReais(descontoReais)
                .descontoPercentual(descontoPercentual)
                .markupAquisicao(markupAquisicao)
                .pisoDesejado(pisoDesejado)
                .pisoAutonomia(pisoAutonomiaEfetivo)
                .pisoAbsoluto(pisoAbsoluto)
                .alcada(alcada)
                .possuiCustosPendentes(possuiPendentes)
                .motivosAlcada(Collections.unmodifiableList(motivos))
                .build();
    }

    private BigDecimal validarPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException(campo + " deve ser maior que zero.");
        }
        return valor;
    }

    private BigDecimal validarNaoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException(campo + " não pode ser nulo ou negativo.");
        }
        return valor;
    }
}
