package com.postech.restaurantes.domain.vo;

import com.postech.restaurantes.domain.Guard;

/**
 * Telefone, só com dígitos: de 10 (fixo com DDD) a 13 (celular com DDI 55 e DDD). Aceita entrada
 * com máscara — {@code (11) 91234-5678}, {@code +55 11 91234-5678} — e guarda os dígitos, que cabem
 * na coluna {@code varchar(20)} do modelo.
 */
public record Phone(String value) {

    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 13;

    public Phone {
        value = Guard.requireNonBlank(value, "Telefone inválido").replaceAll("\\D", "");
        Guard.require(value.length() >= MIN_LENGTH && value.length() <= MAX_LENGTH,
                "Telefone deve ter de " + MIN_LENGTH + " a " + MAX_LENGTH + " dígitos, com DDD");
    }

    public static Phone of(String value) {
        return new Phone(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
