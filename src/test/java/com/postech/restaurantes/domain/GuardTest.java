package com.postech.restaurantes.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuardTest {

    @Test
    @DisplayName("Devolve o próprio valor quando não é nulo")
    void deveDevolverValorQuandoNaoNulo() {
        Object value = new Object();

        Object result = Guard.requireNonNull(value, "msg");

        assertSame(value, result);
    }

    @Test
    @DisplayName("Recusa valor nulo com a mensagem informada")
    void deveRecusarQuandoNulo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Guard.requireNonNull(null, "Campo obrigatório"));

        assertEquals("Campo obrigatório", ex.getMessage());
    }

    @Test
    @DisplayName("Devolve o texto sem espaços nas bordas quando não está em branco")
    void deveDevolverTextoAparadoQuandoNaoBranco() {
        String result = Guard.requireNonBlank("  valor  ", "msg");

        assertEquals("valor", result);
    }

    @Test
    @DisplayName("Recusa texto nulo")
    void deveRecusarTextoNulo() {
        assertThrows(IllegalArgumentException.class, () -> Guard.requireNonBlank(null, "msg"));
    }

    @Test
    @DisplayName("Recusa texto composto só de espaços")
    void deveRecusarTextoEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> Guard.requireNonBlank("   ", "msg"));
    }

    @Test
    @DisplayName("Aceita condição verdadeira sem lançar")
    void deveAceitarCondicaoVerdadeira() {
        Guard.require(true, "msg");
    }

    @Test
    @DisplayName("Recusa condição falsa com a mensagem informada")
    void deveRecusarCondicaoFalsa() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Guard.require(false, "Condição violada"));

        assertEquals("Condição violada", ex.getMessage());
    }

    @Test
    @DisplayName("Normaliza opcional: nulo permanece nulo")
    void deveManterNuloQuandoOpcionalNulo() {
        assertNull(Guard.trimToNull(null));
    }

    @Test
    @DisplayName("Normaliza opcional: em branco vira nulo")
    void deveConverterBrancoEmNulo() {
        assertNull(Guard.trimToNull("   "));
    }

    @Test
    @DisplayName("Normaliza opcional: texto é aparado")
    void deveApararTextoOpcional() {
        assertEquals("abc", Guard.trimToNull(" abc "));
    }
}
