package com.postech.restaurantes.domain.entity.location.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CityTest {

    private static final UUID STATE_ID = UUID.randomUUID();
    private static final com.postech.restaurantes.domain.entity.location.state.State STATE =
            com.postech.restaurantes.domain.entity.location.state.State.restore(STATE_ID, "São Paulo", "SP");

    @Test
    @DisplayName("Rejects a city referencing a state without a persisted id")
    void shouldRejectTransientState() {
        var state = com.postech.restaurantes.domain.entity.location.state.State.create("São Paulo", "SP");
        assertThrows(com.postech.restaurantes.domain.exception.InvariantViolationException.class,
                () -> City.create(state, "São Paulo"));
    }

    @Test
    @DisplayName("Cria cidade vinculada a um estado")
    void deveCriarQuandoValida() {
        City city = City.create(STATE, "São Paulo");

        assertNull(city.getId());
        assertEquals(STATE_ID, city.getStateId());
        assertEquals("São Paulo", city.getName());
    }

    @Test
    @DisplayName("Restaura cidade com id")
    void deveRestaurarComId() {
        UUID id = UUID.randomUUID();

        City city = City.restore(id, STATE, "Campinas");

        assertEquals(id, city.getId());
        assertEquals(STATE_ID, city.getStateId());
        assertEquals("Campinas", city.getName());
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarSemId() {
        assertThrows(IllegalArgumentException.class, () -> City.restore(null, STATE, "Campinas"));
    }

    @Test
    @DisplayName("Recusa criação sem estado")
    void deveRecusarCriarSemEstado() {
        assertThrows(IllegalArgumentException.class, () -> City.create(null, "Campinas"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Recusa nome em branco")
    void deveRecusarNomeEmBranco(String name) {
        assertThrows(IllegalArgumentException.class, () -> City.create(STATE, name));
    }

    @Test
    @DisplayName("Setters revalidam sem corromper a cidade")
    void deveRevalidarNosSetters() {
        City city = City.create(STATE, "Campinas");

        assertThrows(IllegalArgumentException.class, () -> city.setState(null));
        assertThrows(IllegalArgumentException.class, () -> city.setName(""));
        assertEquals(STATE_ID, city.getStateId());
        assertEquals("Campinas", city.getName());
    }
}
