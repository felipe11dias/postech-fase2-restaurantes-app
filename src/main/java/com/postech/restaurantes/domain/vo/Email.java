package com.postech.restaurantes.domain.vo;

import com.postech.restaurantes.domain.Guard;
import java.util.regex.Pattern;

/**
 * Endereço de e-mail válido e normalizado para minúsculas. A normalização acontece na
 * construção, então a regra de e-mail único é correta por construção: {@code Joao@x.com} e
 * {@code joao@x.com} produzem o mesmo valor.
 */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_LENGTH = 255;

    public Email {
        value = Guard.requireNonBlank(value, "E-mail inválido").toLowerCase();
        Guard.require(value.length() <= MAX_LENGTH, "E-mail excede " + MAX_LENGTH + " caracteres");
        Guard.require(FORMAT.matcher(value).matches(), "E-mail inválido");
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
