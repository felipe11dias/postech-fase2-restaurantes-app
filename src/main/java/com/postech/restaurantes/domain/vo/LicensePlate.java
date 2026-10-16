package com.postech.restaurantes.domain.vo;

import com.postech.restaurantes.domain.Guard;
import java.util.regex.Pattern;

/**
 * Placa de veículo: padrão antigo ({@code ABC1234}) ou Mercosul ({@code ABC1D23}), guardada em
 * maiúsculas e sem hífen. Aceita {@code abc-1234} e {@code ABC 1D23}.
 */
public record LicensePlate(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}");

    public LicensePlate {
        value = Guard.requireNonBlank(value, "Placa inválida").toUpperCase().replaceAll("[-\\s]", "");
        Guard.require(FORMAT.matcher(value).matches(), "Placa deve seguir o padrão ABC1234 ou ABC1D23");
    }

    public static LicensePlate of(String value) {
        return new LicensePlate(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
