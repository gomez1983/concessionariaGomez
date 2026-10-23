package com.concessionaria.gomez.domain.service;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.repository.CarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ResumoEstoqueService {
    private final CarroRepository repository;

    public record Resumo(long total, long ativos, long zeroKm, BigDecimal investido,
                         BigDecimal sugerido, BigDecimal margem, Map<String, Long> marcas) { }

    @Transactional(readOnly = true)
    public Resumo consultar() {
        var carros = repository.findAll();
        var ativos = carros.stream().filter(c -> Boolean.TRUE.equals(c.getAtivo())).toList();
        BigDecimal investido = ativos.stream().map(Carro::getCompra).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sugerido = ativos.stream().map(Carro::calcularPrecoVendaSugerido).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> marcas = new TreeMap<>();
        carros.forEach(c -> marcas.merge(c.getMarca(), 1L, Long::sum));
        return new Resumo(carros.size(), ativos.size(), carros.stream().filter(Carro::isZeroKm).count(), investido, sugerido, sugerido.subtract(investido), marcas);
    }
}
