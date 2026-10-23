package com.concessionaria.gomez.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImagemValidator.class)
public @interface ImagemValida {
    String message() default "Envie uma imagem JPEG ou PNG válida de até 10 MB e 40 megapixels.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
