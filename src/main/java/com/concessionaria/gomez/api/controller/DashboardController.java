package com.concessionaria.gomez.api.controller;

import com.concessionaria.gomez.api.model.DashboardModel;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import com.concessionaria.gomez.domain.service.ResumoEstoqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {
    private final ResumoEstoqueService resumoEstoque;

    @GetMapping
    public DashboardModel consultar(
            @RequestHeader(value = "X-Operador-Perfil", required = false) String perfilHeader) {

        PerfilOperadorComercial perfil = PerfilOperadorComercial.fromHeader(perfilHeader);
        boolean podeVerInterno = perfil.podeVerCustosEMargensInternas();

        var resumo = resumoEstoque.consultar();
        return new DashboardModel(
                resumo.total(),
                resumo.ativos(),
                resumo.zeroKm(),
                podeVerInterno ? resumo.investido() : null,
                resumo.sugerido(),
                podeVerInterno ? resumo.margem() : null,
                resumo.marcas(),
                resumo.propostasEmNegociacao(),
                resumo.propostasPendentesAprovacao(),
                resumo.propostasAprovadas(),
                resumo.propostasConvertidas(),
                resumo.faturamentoRealizado(),
                podeVerInterno ? resumo.contribuicaoRealizada() : null);
    }
}
