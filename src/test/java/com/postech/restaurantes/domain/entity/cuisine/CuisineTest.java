package com.postech.restaurantes.domain.entity.cuisine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CuisineTest {

    @Test
    @DisplayName("Cria tipo de cozinha novo, sem id nem datas, com o nome sem espaços nas pontas")
    void deveCriarQuandoValido() {
        Cuisine cuisine = Cuisine.create("  Italiana  ");

        assertNull(cuisine.getId());
        assertNull(cuisine.getCreatedAt());
        assertNull(cuisine.getLastUpdatedAt());
        assertEquals("Italiana", cuisine.getName());
    }

    @Test
    @DisplayName("Restaura tipo de cozinha com id e datas")
    void deveRestaurarComIdEDatas() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime alterado = LocalDateTime.of(2026, 1, 2, 10, 0);

        Cuisine cuisine = Cuisine.restore(id, "Japonesa", criado, alterado);

        assertEquals(id, cuisine.getId());
        assertEquals("Japonesa", cuisine.getName());
        assertEquals(criado, cuisine.getCreatedAt());
        assertEquals(alterado, cuisine.getLastUpdatedAt());
    }

    @Test
    @DisplayName("Recusa restaurar sem id")
    void deveRecusarRestaurarSemId() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> Cuisine.restore(null, "Japonesa", null, null));

        assertEquals("Id do tipo de cozinha inválido", erro.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Recusa nome nulo, vazio ou só com espaços")
    void deveRecusarNomeEmBranco(String nome) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> Cuisine.create(nome));

        assertEquals("Nome do tipo de cozinha inválido", erro.getMessage());
    }

    @Test
    @DisplayName("Troca o nome validando de novo; troca recusada mantém o nome anterior")
    void deveRevalidarAoTrocarONome() {
        Cuisine cuisine = Cuisine.create("Italiana");

        cuisine.setName(" Mexicana ");

        assertEquals("Mexicana", cuisine.getName());
        assertThrows(IllegalArgumentException.class, () -> cuisine.setName(""));
        assertEquals("Mexicana", cuisine.getName());
    }
}
