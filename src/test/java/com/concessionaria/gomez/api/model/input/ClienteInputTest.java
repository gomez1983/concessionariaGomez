package com.concessionaria.gomez.api.model.input;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteInputTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Deve validar ClienteInput com dados válidos")
    void deveAceitarClienteInputValido() {
        ClienteInput input = new ClienteInput();
        input.setNome("Lucas Mendes");
        input.setCpfCnpj("12345678901");
        input.setEmail("lucas@exemplo.com");
        input.setTelefone("(11) 98888-7777");

        var violations = validator.validate(input);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar quando nome ou cpfCnpj estiverem em branco")
    void deveRejeitarCamposObrigatoriosEmBranco() {
        ClienteInput input = new ClienteInput();
        input.setNome("   ");
        input.setCpfCnpj("");

        var violations = validator.validate(input);
        assertThat(violations).hasSize(2);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nome"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cpfCnpj"));
    }

    @Test
    @DisplayName("Deve rejeitar e-mail com formato inválido")
    void deveRejeitarEmailInvalido() {
        ClienteInput input = new ClienteInput();
        input.setNome("Lucas Mendes");
        input.setCpfCnpj("12345678901");
        input.setEmail("email-invalido-sem-arroba");

        var violations = validator.validate(input);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Deve rejeitar estado com mais ou menos de 2 caracteres no endereço")
    void deveRejeitarEstadoComTamanhoInvalido() {
        ClienteInput input = new ClienteInput();
        input.setNome("Lucas Mendes");
        input.setCpfCnpj("12345678901");

        EnderecoInput endereco = new EnderecoInput();
        endereco.setEstado("SAOPAULO"); // Inválido, deve ter 2 caracteres
        input.setEndereco(endereco);

        var violations = validator.validate(input);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("endereco.estado"));
    }
}
