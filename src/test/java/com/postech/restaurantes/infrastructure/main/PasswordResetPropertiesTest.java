package com.postech.restaurantes.infrastructure.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordResetPropertiesTest {

    @Test
    @DisplayName("A validade configurada em minutos vira a duração usada pelo caso de uso")
    void deveConverterAValidade() {
        assertEquals(Duration.ofMinutes(30), new PasswordResetProperties(30).tokenValidity());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Validade não positiva é recusada na configuração")
    void deveRecusarValidadeNaoPositiva(int minutos) {
        assertThrows(IllegalArgumentException.class, () -> new PasswordResetProperties(minutos));
    }
}
