package com.postech.restaurantes.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ZipCodeTest {

    @Test
    @DisplayName("Aceita CEP com hífen e guarda só os dígitos")
    void deveGuardarDigitosQuandoComHifen() {
        ZipCode zipCode = ZipCode.of("01001-000");

        assertEquals("01001000", zipCode.value());
        assertEquals("01001000", zipCode.toString());
    }

    @Test
    @DisplayName("Aceita CEP só com dígitos")
    void deveAceitarQuandoSoDigitos() {
        assertEquals("01001000", ZipCode.of("01001000").value());
    }

    @Test
    @DisplayName("Formata como 00000-000 sob demanda")
    void deveFormatarComHifen() {
        assertEquals("01001-000", ZipCode.of("01001000").formatted());
    }

    @Test
    @DisplayName("CEPs com e sem máscara são o mesmo valor")
    void deveSerIgualIndependenteDaMascara() {
        assertEquals(ZipCode.of("01001-000"), ZipCode.of("01001000"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "1234567", "123456789", "abcdefgh", "0100-100"})
    @DisplayName("Recusa CEP nulo, em branco ou sem exatamente 8 dígitos")
    void deveRecusarQuandoInvalido(String value) {
        assertThrows(IllegalArgumentException.class, () -> ZipCode.of(value));
    }
}
