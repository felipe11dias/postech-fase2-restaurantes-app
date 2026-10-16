package com.postech.restaurantes;

import java.util.concurrent.ThreadLocalRandom;

/**
 * CPF, CNPJ e CNH válidos e aleatórios para os testes de integração. O banco é compartilhado entre as
 * classes de teste e os documentos são únicos, então cada teste gera os seus em vez de repetir um
 * valor fixo — como faz com e-mail e login, pela marca única.
 */
public final class Documentos {

    private Documentos() {
    }

    /** CPF de 11 dígitos, sem máscara, com os verificadores certos. */
    public static String cpf() {
        String base = digitos(9);
        String comPrimeiro = base + verificadorCpf(base);
        return comPrimeiro + verificadorCpf(comPrimeiro);
    }

    /** CNPJ numérico de 14 dígitos, sem máscara, com os verificadores certos. */
    public static String cnpj() {
        String base = digitos(12);
        String comPrimeiro = base + verificadorCnpj(base, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return comPrimeiro + verificadorCnpj(comPrimeiro, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    /** Número de CNH: 11 dígitos (o domínio só confere o formato). */
    public static String cnh() {
        return digitos(11);
    }

    /** Dígitos aleatórios, nunca todos iguais (sequência repetida não é documento). */
    private static String digitos(int quantidade) {
        StringBuilder valor = new StringBuilder();
        for (int i = 0; i < quantidade; i++) {
            valor.append(ThreadLocalRandom.current().nextInt(10));
        }
        return valor.chars().distinct().count() > 1 ? valor.toString() : digitos(quantidade);
    }

    private static int verificadorCpf(String valor) {
        int soma = 0;
        for (int i = 0; i < valor.length(); i++) {
            soma += (valor.charAt(i) - '0') * (valor.length() + 1 - i);
        }
        int digito = 11 - soma % 11;
        return digito >= 10 ? 0 : digito;
    }

    private static int verificadorCnpj(String valor, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (valor.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
