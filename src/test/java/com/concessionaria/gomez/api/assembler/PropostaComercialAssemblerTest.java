package com.concessionaria.gomez.api.assembler;

import com.concessionaria.gomez.api.model.input.PropostaComercialInput;
import com.concessionaria.gomez.domain.model.BaseCalculoItemFinanceiro;
import com.concessionaria.gomez.domain.model.ItemFinanceiroRevisao;
import com.concessionaria.gomez.domain.model.NaturezaItemFinanceiro;
import com.concessionaria.gomez.domain.model.PerfilOperadorComercial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropostaComercialAssemblerTest {

    private final PropostaComercialAssembler assembler = new PropostaComercialAssembler();

    @Test
    @DisplayName("Receita adicional declarada por VENDEDOR deve entrar pendente de confirmação")
    void receitaAdicionalDoVendedorDeveEntrarPendente() {
        PropostaComercialInput input = inputCom(
                item(BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL, true));

        ItemFinanceiroRevisao item = unicoItem(input, PerfilOperadorComercial.VENDEDOR);

        assertThat(item.getConfirmado()).isFalse();
        assertThat(item.isPendenteConfirmacao()).isTrue();
    }

    @Test
    @DisplayName("Receita adicional sem perfil identificado deve entrar pendente de confirmação")
    void receitaAdicionalSemPerfilDeveEntrarPendente() {
        PropostaComercialInput input = inputCom(
                item(BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL, true));

        assertThat(unicoItem(input, null).getConfirmado()).isFalse();
    }

    @Test
    @DisplayName("Receita adicional confirmada por GERENTE ou ADMIN deve ser mantida como confirmada")
    void receitaAdicionalDoGerenteDeveSerConfirmada() {
        PropostaComercialInput input = inputCom(
                item(BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL, true));

        assertThat(unicoItem(input, PerfilOperadorComercial.GERENTE).getConfirmado()).isTrue();
        assertThat(unicoItem(input, PerfilOperadorComercial.ADMIN).getConfirmado()).isTrue();
    }

    @Test
    @DisplayName("Receita adicional não confirmada pelo GERENTE deve continuar pendente")
    void receitaAdicionalNaoConfirmadaDeveContinuarPendente() {
        PropostaComercialInput input = inputCom(
                item(BaseCalculoItemFinanceiro.RECEITA_ADICIONAL_ELEGIVEL, false));

        assertThat(unicoItem(input, PerfilOperadorComercial.GERENTE).getConfirmado()).isFalse();
    }

    @Test
    @DisplayName("Custos declarados pelo VENDEDOR não devem ser afetados pela regra de receita adicional")
    void custosDoVendedorDevemManterConfirmacao() {
        PropostaComercialInput input = inputCom(
                item(BaseCalculoItemFinanceiro.VALOR_FIXO_REAIS, true));

        assertThat(unicoItem(input, PerfilOperadorComercial.VENDEDOR).getConfirmado()).isTrue();
    }

    private ItemFinanceiroRevisao unicoItem(PropostaComercialInput input, PerfilOperadorComercial perfil) {
        List<ItemFinanceiroRevisao> itens = assembler
                .toCommand(input, "vendedor@concessionariagomez.com.br", perfil)
                .getItensExtras();
        assertThat(itens).hasSize(1);
        return itens.get(0);
    }

    private static PropostaComercialInput inputCom(PropostaComercialInput.ItemFinanceiroInput item) {
        PropostaComercialInput input = new PropostaComercialInput();
        input.setCarroId(1L);
        input.setClienteId(1L);
        input.setPrecoNegociado(new BigDecimal("80000.00"));
        input.setItensExtras(List.of(item));
        return input;
    }

    private static PropostaComercialInput.ItemFinanceiroInput item(
            BaseCalculoItemFinanceiro baseCalculo, boolean confirmado) {
        PropostaComercialInput.ItemFinanceiroInput item = new PropostaComercialInput.ItemFinanceiroInput();
        item.setNatureza(NaturezaItemFinanceiro.RECEITA_ACESSORIA_ELEGIVEL);
        item.setBaseCalculo(baseCalculo);
        item.setValor(new BigDecimal("50000.00"));
        item.setConfirmado(confirmado);
        item.setDescricao("Item de teste");
        return item;
    }
}
