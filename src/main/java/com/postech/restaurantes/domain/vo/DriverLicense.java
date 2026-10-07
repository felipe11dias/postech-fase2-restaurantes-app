package com.postech.restaurantes.domain.vo;

import com.postech.restaurantes.domain.Guard;

/**
 * Número de registro da CNH: 11 dígitos, guardado sem máscara. Só o formato é conferido — os
 * dígitos verificadores da CNH têm variantes de cálculo conforme a época de emissão, e a
 * autenticidade do documento é do órgão de trânsito, não deste sistema. Sequências de um só dígito
 * são recusadas.
 */
public record DriverLicense(String value) {

    private static final int LENGTH = 11;

    public DriverLicense {
        value = Guard.requireNonBlank(value, "CNH inválida").replaceAll("\\D", "");
        Guard.require(value.length() == LENGTH, "CNH deve ter " + LENGTH + " dígitos");
        Guard.require(value.chars().distinct().count() > 1, "CNH inválida");
    }

    public static DriverLicense of(String value) {
        return new DriverLicense(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
