package com.postech.restaurantes.domain.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileTypeTest {

    @Test
    @DisplayName("Os tipos são os quatro perfis do modelo, cada um com o nome usado nas mensagens")
    void deveTerOsQuatroPerfis() {
        assertEquals(List.of("dono", "cliente", "entregador", "administrador"),
                List.of(ProfileType.values()).stream().map(ProfileType::description).toList());
    }

    @Test
    @DisplayName("Converte o nome sem diferenciar maiúsculas, como chega no caminho da URL")
    void deveConverterSemDiferenciarMaiusculas() {
        assertEquals(ProfileType.OWNER, ProfileType.from("owner"));
        assertEquals(ProfileType.COURIER, ProfileType.from("Courier"));
        assertEquals(ProfileType.ADMIN, ProfileType.from("ADMIN"));
    }

    @Test
    @DisplayName("Recusa tipo desconhecido com mensagem do domínio")
    void deveRecusarTipoDesconhecido() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> ProfileType.from("gerente"));

        assertEquals("Tipo de perfil inválido: gerente", erro.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("Recusa tipo em branco")
    void deveRecusarTipoEmBranco(String tipo) {
        assertThrows(IllegalArgumentException.class, () -> ProfileType.from(tipo));
    }
}
