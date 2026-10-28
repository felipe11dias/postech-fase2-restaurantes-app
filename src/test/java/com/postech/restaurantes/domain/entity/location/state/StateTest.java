package com.postech.restaurantes.domain.entity.location.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StateTest {

    @Test
    @DisplayName("Cria estado sem id e normaliza a sigla")
    void deveCriarQuandoValido() {
        State state = State.create("São Paulo", "sp");

        assertNull(state.getId());
        assertEquals("São Paulo", state.getName());
        assertEquals("SP", state.getAcronym());
    }

    @Test
    @DisplayName("Restaura estado com id")
    void deveRestaurarComId() {
        UUID id = UUID.randomUUID();

        State state = State.restore(id, "Rio de Janeiro", "rj");

        assertEquals(id, state.getId());
        assertEquals("RJ", state.getAcronym());
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarSemId() {
        assertThrows(IllegalArgumentException.class, () -> State.restore(null, "São Paulo", "SP"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Recusa nome em branco")
    void deveRecusarNomeEmBranco(String name) {
        assertThrows(IllegalArgumentException.class, () -> State.create(name, "SP"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "S", "SPX", "S1", "12"})
    @DisplayName("Recusa sigla diferente de duas letras")
    void deveRecusarSiglaInvalida(String acronym) {
        assertThrows(IllegalArgumentException.class, () -> State.create("São Paulo", acronym));
    }

    @Test
    @DisplayName("Setters revalidam sem corromper o estado")
    void deveRevalidarNosSetters() {
        State state = State.create("São Paulo", "SP");

        assertThrows(IllegalArgumentException.class, () -> state.setName(""));
        assertThrows(IllegalArgumentException.class, () -> state.setAcronym("X"));
        assertEquals("São Paulo", state.getName());
        assertEquals("SP", state.getAcronym());
    }
}
