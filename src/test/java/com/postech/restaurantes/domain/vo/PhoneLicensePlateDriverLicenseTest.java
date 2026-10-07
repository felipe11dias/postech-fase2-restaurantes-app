package com.postech.restaurantes.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** VOs de documento e contato usados pelos perfis: telefone, placa e CNH. */
class PhoneLicensePlateDriverLicenseTest {

    @Nested
    @DisplayName("Telefone")
    class Telefone {

        @ParameterizedTest
        @ValueSource(strings = {"(11) 3123-4567", "(11) 91234-5678", "+55 11 91234-5678", "551131234567"})
        @DisplayName("Aceita fixo e celular, com ou sem DDI, e guarda só os dígitos")
        void deveGuardarSoOsDigitos(String valor) {
            assertEquals(valor.replaceAll("\\D", ""), Phone.of(valor).value());
        }

        @Test
        @DisplayName("Telefones com e sem máscara são o mesmo valor")
        void deveSerIgualIndependenteDaMascara() {
            assertEquals(Phone.of("(11) 91234-5678"), Phone.of("11912345678"));
            assertEquals("11912345678", Phone.of("11912345678").toString());
        }

        @ParameterizedTest
        @ValueSource(strings = {"912345678", "55119123456789", "abc"})
        @DisplayName("Recusa menos de 10 ou mais de 13 dígitos")
        void deveRecusarTamanhoErrado(String valor) {
            assertThrows(IllegalArgumentException.class, () -> Phone.of(valor));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Recusa telefone em branco")
        void deveRecusarEmBranco(String valor) {
            assertThrows(IllegalArgumentException.class, () -> Phone.of(valor));
        }
    }

    @Nested
    @DisplayName("Placa")
    class Placa {

        @Test
        @DisplayName("Aceita o padrão antigo e o Mercosul, guardando em maiúsculas e sem hífen")
        void deveNormalizarOsDoisPadroes() {
            assertEquals("ABC1234", LicensePlate.of("abc-1234").value());
            assertEquals("ABC1D23", LicensePlate.of("ABC 1D23").value());
            assertEquals("ABC1D23", LicensePlate.of("abc1d23").toString());
        }

        @ParameterizedTest
        @ValueSource(strings = {"AB12345", "ABCD123", "ABC12D3", "ABC123", "ABC12345"})
        @DisplayName("Recusa placa fora dos dois padrões")
        void deveRecusarFormatoErrado(String valor) {
            assertThrows(IllegalArgumentException.class, () -> LicensePlate.of(valor));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Recusa placa em branco")
        void deveRecusarEmBranco(String valor) {
            assertThrows(IllegalArgumentException.class, () -> LicensePlate.of(valor));
        }
    }

    @Nested
    @DisplayName("CNH")
    class Cnh {

        @Test
        @DisplayName("Aceita 11 dígitos, com ou sem máscara, e guarda só os dígitos")
        void deveGuardarSoOsDigitos() {
            assertEquals("02650306461", DriverLicense.of("026.503.064-61").value());
            assertEquals(DriverLicense.of("02650306461"), DriverLicense.of("026.503.064-61"));
            assertEquals("02650306461", DriverLicense.of("02650306461").toString());
        }

        @ParameterizedTest
        @ValueSource(strings = {"0265030646", "026503064611", "11111111111"})
        @DisplayName("Recusa CNH sem 11 dígitos ou com um só dígito repetido")
        void deveRecusarInvalida(String valor) {
            assertThrows(IllegalArgumentException.class, () -> DriverLicense.of(valor));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Recusa CNH em branco")
        void deveRecusarEmBranco(String valor) {
            assertThrows(IllegalArgumentException.class, () -> DriverLicense.of(valor));
        }
    }
}
