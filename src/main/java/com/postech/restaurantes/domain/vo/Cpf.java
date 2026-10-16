package com.postech.restaurantes.domain.vo;

import com.postech.restaurantes.domain.Guard;

/**
 * CPF: 11 dígitos, armazenado sem máscara, com os dois dígitos verificadores conferidos. Aceita
 * entrada com ou sem máscara ({@code 529.982.247-25} ou {@code 52998224725}). Sequências de um só
 * dígito ({@code 111.111.111-11}) passam na conta dos verificadores, mas não são CPF — são recusadas.
 */
public record Cpf(String value) {

    private static final int LENGTH = 11;

    public Cpf {
        value = Guard.requireNonBlank(value, "CPF inválido").replaceAll("\\D", "");
        Guard.require(value.length() == LENGTH, "CPF deve ter " + LENGTH + " dígitos");
        Guard.require(value.chars().distinct().count() > 1, "CPF inválido");
        Guard.require(digito(value, 9) == value.charAt(9) - '0' && digito(value, 10) == value.charAt(10) - '0',
                "CPF inválido");
    }

    public static Cpf of(String value) {
        return new Cpf(value);
    }

    /** Dígito verificador da posição {@code posicao} (9 ou 10), pelos pesos decrescentes a partir de posicao + 1. */
    private static int digito(String digitos, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Formato {@code 000.000.000-00}. */
    public String formatted() {
        return value.substring(0, 3) + "." + value.substring(3, 6) + "." + value.substring(6, 9) + "-"
                + value.substring(9);
    }

    @Override
    public String toString() {
        return value;
    }
}
