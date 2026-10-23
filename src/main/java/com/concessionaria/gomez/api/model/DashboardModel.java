package com.concessionaria.gomez.api.model;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardModel(long totalVeiculos, long ativos, long zeroKm,
                             BigDecimal valorInvestido, BigDecimal vendaSugerida, BigDecimal margemPrevista,
                             Map<String, Long> marcas) { }
