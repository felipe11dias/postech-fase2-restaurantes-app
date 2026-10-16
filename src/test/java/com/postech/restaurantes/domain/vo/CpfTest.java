package com.postech.restaurantes.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

    @Test
    @DisplayName("Aceita CPF com máscara e guarda só os dígitos")
    void deveGuardarDigitosQuandoComMascara() {
        Cpf cpf = Cpf.of("529.982.247-25");

        assertEquals("52998224725", cpf.value());
        assertEquals("52998224725", cpf.toString());
    }

    @Test
    @DisplayName("CPFs com e sem máscara são o mesmo valor, e a máscara volta sob demanda")
    void deveSerIgualIndependenteDaMascara() {
        assertEquals(Cpf.of("111.444.777-35"), Cpf.of("11144477735"));
        assertEquals("111.444.777-35", Cpf.of("11144477735").formatted());
    }

    @Test
    @DisplayName("Aceita CPF cujo verificador é zero (resto da divisão menor que 2)")
    void deveAceitarVerificadorZero() {
        assertEquals("12345678909", Cpf.of("123.456.789-09").value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-26", "529.982.247-15", "11144477734"})
    @DisplayName("Recusa CPF com dígito verificador errado")
    void deveRecusarDigitoVerificadorErrado(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cpf.of(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "123456789012", "abc"})
    @DisplayName("Recusa CPF sem 11 dígitos")
    void deveRecusarTamanhoErrado(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cpf.of(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"000.000.000-00", "11111111111", "99999999999"})
    @DisplayName("Recusa sequência de um só dígito, mesmo passando na conta dos verificadores")
    void deveRecusarDigitosRepetidos(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cpf.of(valor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Recusa CPF em branco")
    void deveRecusarEmBranco(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cpf.of(valor));
    }
}
