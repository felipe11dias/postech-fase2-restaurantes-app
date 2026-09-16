package com.postech.restaurantes.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    @DisplayName("Cria e-mail válido normalizando para minúsculas e removendo espaços")
    void deveNormalizarQuandoValido() {
        Email email = Email.of("  Joao.Silva@Email.COM ");

        assertEquals("joao.silva@email.com", email.value());
        assertEquals("joao.silva@email.com", email.toString());
    }

    @Test
    @DisplayName("Dois e-mails que diferem só por maiúsculas são o mesmo valor")
    void deveSerIgualQuandoDifereApenasEmCaixa() {
        assertEquals(Email.of("Joao@x.com"), Email.of("joao@x.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "semarroba.com", "@dominio.com", "usuario@", "usuario@dominio", "a b@x.com", "a@b c.com"})
    @DisplayName("Recusa e-mail nulo, em branco ou com formato inválido")
    void deveRecusarQuandoInvalido(String value) {
        assertThrows(IllegalArgumentException.class, () -> Email.of(value));
    }

    @Test
    @DisplayName("Recusa e-mail com mais de 255 caracteres")
    void deveRecusarQuandoExcedeTamanho() {
        String longo = "a".repeat(250) + "@x.com";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Email.of(longo));

        assertEquals("E-mail excede 255 caracteres", ex.getMessage());
    }
}
