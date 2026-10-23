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
        input.setQuilometragem(50000);
        input.setDataCompra(OffsetDateTime.now().plusDays(2));

        var violations = validator.validate(input);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dataCompra")));
    }

    @Test
    @DisplayName("Deve aceitar quando dataCompra for no passado ou nula e quilometragem for válida")
    void deveAceitarDataCompraValidaOuNula() {
        CarroInput input = new CarroInput();
        input.setMarca("Ford");
        input.setModelo("Ka");
        input.setAno(2020);
        input.setCompra(new BigDecimal("30000.00"));
        input.setQuilometragem(50000);
        input.setDataCompra(OffsetDateTime.now().minusDays(1));

        var violations = validator.validate(input);
        assertTrue(violations.isEmpty());

        input.setDataCompra(null);
        var violationsComDataNula = validator.validate(input);
        assertTrue(violationsComDataNula.isEmpty());
    }

    @Test
    @DisplayName("Deve rejeitar quando quilometragem for negativa ou nula")
    void deveRejeitarQuilometragemInvalida() {
        CarroInput input = new CarroInput();
        input.setMarca("Ford");
        input.setModelo("Ka");
        input.setAno(2020);
        input.setCompra(new BigDecimal("30000.00"));
        input.setQuilometragem(-100);

        var violationsNegativa = validator.validate(input);
        assertFalse(violationsNegativa.isEmpty());
        assertTrue(violationsNegativa.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quilometragem")));

        input.setQuilometragem(null);
        var violationsNula = validator.validate(input);
        assertFalse(violationsNula.isEmpty());
        assertTrue(violationsNula.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quilometragem")));
    }

    @Test
    @DisplayName("Deve aceitar quilometragem zero (veículo 0 km)")
    void deveAceitarQuilometragemZero() {
        CarroInput input = new CarroInput();
        input.setMarca("Toyota");
        input.setModelo("Corolla");
        input.setAno(2024);
        input.setCompra(new BigDecimal("150000.00"));
        input.setQuilometragem(0);

        var violations = validator.validate(input);
        assertTrue(violations.isEmpty());
    }
}
