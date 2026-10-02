package com.concessionaria.gomez.api.model.input;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarroInputTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Deve rejeitar quando dataCompra for no futuro")
    void deveRejeitarDataCompraFutura() {
        CarroInput input = new CarroInput();
        input.setMarca("Ford");
        input.setModelo("Ka");
        input.setAno(2020);
        input.setCompra(new BigDecimal("30000.00"));
        input.setDataCompra(OffsetDateTime.now().plusDays(2));

        var violations = validator.validate(input);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dataCompra")));
    }

    @Test
    @DisplayName("Deve aceitar quando dataCompra for no passado ou nula")
    void deveAceitarDataCompraValidaOuNula() {
        CarroInput input = new CarroInput();
        input.setMarca("Ford");
        input.setModelo("Ka");
        input.setAno(2020);
        input.setCompra(new BigDecimal("30000.00"));
        input.setDataCompra(OffsetDateTime.now().minusDays(1));

        var violations = validator.validate(input);
        assertTrue(violations.isEmpty());

        input.setDataCompra(null);
        var violationsComDataNula = validator.validate(input);
        assertTrue(violationsComDataNula.isEmpty());
    }
}
