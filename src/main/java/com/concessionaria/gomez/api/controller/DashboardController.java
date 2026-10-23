package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.model.DashboardModel;
import com.concessionaria.gomez.domain.service.ResumoEstoqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final ResumoEstoqueService resumoEstoque;

    @GetMapping
    public DashboardModel consultar() {
        var resumo = resumoEstoque.consultar();
        return new DashboardModel(resumo.total(), resumo.ativos(), resumo.zeroKm(), resumo.investido(), resumo.sugerido(), resumo.margem(), resumo.marcas());
    }
}
