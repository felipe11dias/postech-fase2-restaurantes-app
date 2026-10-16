package com.postech.restaurantes.domain.vo;

import com.postech.restaurantes.domain.Guard;
import java.util.regex.Pattern;

/**
 * CNPJ: 14 posições, armazenado sem máscara, com os dois dígitos verificadores conferidos. Desde
 * julho de 2026 a Receita emite também o CNPJ <em>alfanumérico</em>: as 12 primeiras posições podem
 * ter letras maiúsculas, e os verificadores continuam numéricos. A conta é a mesma para os dois —
 * cada posição vale o código do caractere menos 48 (os dígitos valem o próprio número; {@code A}
 * vale 17) —, então um só algoritmo atende o numérico e o alfanumérico.
 *
 * <p>Aceita entrada com ou sem máscara ({@code 11.222.333/0001-81}, {@code 12.ABC.345/01DE-35}) e
 * em minúsculas. Sequências de um só caractere passam na conta, mas não são CNPJ — são recusadas.
 */
public record Cnpj(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{12}[0-9]{2}");
    private static final int[] PESOS_PRIMEIRO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        value = Guard.requireNonBlank(value, "CNPJ inválido").toUpperCase().replaceAll("[.\\-/\\s]", "");
        Guard.require(FORMAT.matcher(value).matches(),
                "CNPJ deve ter 14 posições: 12 letras ou dígitos e 2 dígitos verificadores");
        Guard.require(value.chars().distinct().count() > 1, "CNPJ inválido");
        Guard.require(digito(value, PESOS_PRIMEIRO) == value.charAt(12) - '0'
                && digito(value, PESOS_SEGUNDO) == value.charAt(13) - '0', "CNPJ inválido");
    }

    public static Cnpj of(String value) {
        return new Cnpj(value);
    }

    private static int digito(String valor, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (valor.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Formato {@code 00.000.000/0000-00} (ou com letras, no alfanumérico). */
    public String formatted() {
        return value.substring(0, 2) + "." + value.substring(2, 5) + "." + value.substring(5, 8) + "/"
                + value.substring(8, 12) + "-" + value.substring(12);
    }

    @Override
    public String toString() {
        return value;
    }
}
