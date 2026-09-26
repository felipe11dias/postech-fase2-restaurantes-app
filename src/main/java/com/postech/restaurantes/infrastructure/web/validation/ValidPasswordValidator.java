package com.postech.restaurantes.infrastructure.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/** Valida {@link ValidPassword}: mínimo em caracteres, máximo em bytes UTF-8. */
public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    static final int MIN_CHARACTERS = 8;

    /** Limite do BCrypt: acima disso o codificador recusa a senha. */
    static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        // Conta pontos de código, e não unidades UTF-16: um emoji é um caractere, não dois.
        return value.codePointCount(0, value.length()) >= MIN_CHARACTERS
                && value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
