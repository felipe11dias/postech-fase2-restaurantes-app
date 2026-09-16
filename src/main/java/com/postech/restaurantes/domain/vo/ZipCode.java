package com.postech.restaurantes.domain.vo;

import com.postech.restaurantes.domain.Guard;

/**
 * CEP brasileiro: exatamente 8 dígitos, armazenado sem máscara. Aceita entrada com ou sem
 * hífen ({@code 01001-000} ou {@code 01001000}); a formatação é aplicada sob demanda.
 */
public record ZipCode(String value) {

    private static final int LENGTH = 8;

    public ZipCode {
        value = Guard.requireNonBlank(value, "CEP inválido").replaceAll("\\D", "");
        Guard.require(value.length() == LENGTH, "CEP deve ter " + LENGTH + " dígitos");
    }

    public static ZipCode of(String value) {
        return new ZipCode(value);
    }

    /** Formato {@code 00000-000}. */
    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }

    @Override
    public String toString() {
        return value;
    }
}
