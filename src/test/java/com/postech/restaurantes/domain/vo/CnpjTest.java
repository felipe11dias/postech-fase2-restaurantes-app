package com.postech.restaurantes.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CnpjTest {

    @Test
    @DisplayName("Aceita CNPJ numérico com máscara e guarda sem ela")
    void deveGuardarSemMascaraQuandoNumerico() {
        Cnpj cnpj = Cnpj.of("11.222.333/0001-81");

        assertEquals("11222333000181", cnpj.value());
        assertEquals("11222333000181", cnpj.toString());
        assertEquals("11.222.333/0001-81", cnpj.formatted());
    }

    @Test
    @DisplayName("Aceita CNPJ alfanumérico (Receita, julho de 2026), em minúsculas, e guarda em maiúsculas")
    void deveAceitarCnpjAlfanumerico() {
        Cnpj cnpj = Cnpj.of("12.abc.345/01de-35");

        assertEquals("12ABC34501DE35", cnpj.value());
        assertEquals("12.ABC.345/01DE-35", cnpj.formatted());
        assertEquals(cnpj, Cnpj.of("12ABC34501DE35"));
    }

    @Test
    @DisplayName("Aceita CNPJ cujo verificador é zero (resto da divisão menor que 2)")
    void deveAceitarVerificadorZero() {
        assertEquals("04252011000110", Cnpj.of("04.252.011/0001-10").value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-82", "11.222.333/0001-91", "12.ABC.345/01DE-36"})
    @DisplayName("Recusa CNPJ com dígito verificador errado, numérico ou alfanumérico")
    void deveRecusarDigitoVerificadorErrado(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cnpj.of(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1122233300018", "112223330001811", "12ABC34501DEAB", "12ABC345-01DE#35"})
    @DisplayName("Recusa CNPJ fora do formato: tamanho errado, verificadores com letra ou caractere estranho")
    void deveRecusarFormatoErrado(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cnpj.of(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00.000.000/0000-00", "11111111111111"})
    @DisplayName("Recusa sequência de um só caractere, mesmo passando na conta dos verificadores")
    void deveRecusarCaracteresRepetidos(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cnpj.of(valor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Recusa CNPJ em branco")
    void deveRecusarEmBranco(String valor) {
        assertThrows(IllegalArgumentException.class, () -> Cnpj.of(valor));
    }
}
