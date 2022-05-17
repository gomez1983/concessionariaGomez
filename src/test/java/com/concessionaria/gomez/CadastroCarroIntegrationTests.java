package com.concessionaria.gomez;

import com.concessionaria.gomez.domain.model.Carro;
import com.concessionaria.gomez.domain.service.CadastroCarroService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class CadastroCarroIntegrationTests {

    @Autowired
    private CadastroCarroService cadastroCarro;

    @Test
    public void testarCadastroCarroComSucesso (){
        // cenário
        Carro novoCarro = new Carro();
        novoCarro.setMarca("Ford");
        novoCarro.setModelo("Fusion");
        novoCarro.setAno(2011);
        novoCarro.setCompra(65000);

        // ação
        novoCarro = cadastroCarro.salvar(novoCarro);

        // validação
        assertThat(novoCarro).isNotNull();
        assertThat(novoCarro.getId()).isNotNull();
    }

    @Test
    public void testarCadastroCarroSemModelo() {
        Carro novoCarro = new Carro();
        novoCarro.setModelo(null);

        ConstraintViolationException erroEsperado =
                Assertions.assertThrows(ConstraintViolationException.class, () -> {
                    cadastroCarro.salvar(novoCarro);
                });

        // validação
        assertThat(erroEsperado).isNotNull();
    }
}