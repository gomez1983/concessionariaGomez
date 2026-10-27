package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.model.*;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import com.concessionaria.gomez.domain.repository.PropostaComercialRepository;
import com.concessionaria.gomez.domain.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ResumoEstoqueService {
    private final CarroRepository repository;
    private final PropostaComercialRepository propostaRepository;
    private final VendaRepository vendaRepository;

    public record Resumo(
            long total,
            long ativos,
            long zeroKm,
            BigDecimal investido,
            BigDecimal sugerido,
            BigDecimal margem,
            Map<String, Long> marcas,
            long propostasEmNegociacao,
            long propostasPendentesAprovacao,
            long propostasAprovadas,
            long propostasConvertidas,
            BigDecimal faturamentoRealizado,
            BigDecimal contribuicaoRealizada) { }

    @Transactional(readOnly = true)
    public Resumo consultar() {
        var carros = repository.findAll();
        var ativos = carros.stream().filter(c -> Boolean.TRUE.equals(c.getAtivo())).toList();
        BigDecimal investido = ativos.stream().map(Carro::getCompra).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sugerido = ativos.stream().map(Carro::calcularPrecoVendaSugerido).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> marcas = new TreeMap<>();
        carros.forEach(c -> marcas.merge(c.getMarca(), 1L, Long::sum));

        List<PropostaComercial> propostas = propostaRepository != null ? propostaRepository.findAll() : List.of();
        long emNegociacao = propostas.stream()
                .filter(p -> p.getStatus() == StatusPropostaComercial.RASCUNHO || p.getStatus() == StatusPropostaComercial.EM_NEGOCIACAO)
                .count();
        long pendentes = propostas.stream()
                .filter(p -> p.getStatus() == StatusPropostaComercial.PENDENTE_APROVACAO)
                .count();
        long aprovadas = propostas.stream()
                .filter(p -> p.getStatus() == StatusPropostaComercial.APROVADA)
                .count();
        long convertidas = propostas.stream()
                .filter(p -> p.getStatus() == StatusPropostaComercial.CONVERTIDA)
                .count();

        List<Venda> vendasAtivas = vendaRepository != null
                ? vendaRepository.findAll().stream().filter(v -> !Boolean.TRUE.equals(v.getCancelada())).toList()
                : List.of();
        BigDecimal faturamentoRealizado = vendasAtivas.stream()
                .map(Venda::getValorVenda)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal contribuicaoRealizada = vendasAtivas.stream()
                .map(v -> v.getRevisaoConvertida() != null
                        ? v.getRevisaoConvertida().getContribuicaoEstimada()
                        : v.getValorVenda().subtract(v.getCarro().getCompra()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Resumo(
                carros.size(),
                ativos.size(),
                carros.stream().filter(Carro::isZeroKm).count(),
                investido,
                sugerido,
                sugerido.subtract(investido),
                marcas,
                emNegociacao,
                pendentes,
                aprovadas,
                convertidas,
                faturamentoRealizado,
                contribuicaoRealizada);
    }
}
