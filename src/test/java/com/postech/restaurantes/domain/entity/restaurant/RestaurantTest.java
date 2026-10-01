package com.postech.restaurantes.domain.entity.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RestaurantTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final LocalTime START = LocalTime.of(8, 0);
    private static final LocalTime END = LocalTime.of(22, 0);

    @Test
    @DisplayName("Cria restaurante válido sem id nem auditoria")
    void deveCriarQuandoValido() {
        Restaurant restaurant = Restaurant.create(USER_ID, ADDRESS_ID, "Sabor & Arte", START, END);

        assertNull(restaurant.getId());
        assertNull(restaurant.getCreatedAt());
        assertNull(restaurant.getLastUpdatedAt());
        assertEquals(USER_ID, restaurant.getUserId());
        assertEquals(ADDRESS_ID, restaurant.getAddressId());
        assertEquals("Sabor & Arte", restaurant.getName());
        assertEquals(START, restaurant.getOfficeHourStart());
        assertEquals(END, restaurant.getOfficeHourEnd());
    }

    @Test
    @DisplayName("Restaura restaurante com id e auditoria conhecidos")
    void deveRestaurarComIdEAuditoria() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime alterado = LocalDateTime.of(2026, 1, 2, 10, 0);

        Restaurant restaurant = Restaurant.restore(id, USER_ID, ADDRESS_ID, "Sabor & Arte", START, END, criado, alterado);

        assertEquals(id, restaurant.getId());
        assertEquals(criado, restaurant.getCreatedAt());
        assertEquals(alterado, restaurant.getLastUpdatedAt());
    }

    @Test
    @DisplayName("Recusa restauração com id nulo")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.restore(null, USER_ID, ADDRESS_ID, "Sabor", START, END, null, null));
    }

    @Test
    @DisplayName("Recusa dono nulo")
    void deveRecusarDonoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(null, ADDRESS_ID, "Sabor", START, END));
    }

    @Test
    @DisplayName("Recusa endereço nulo")
    void deveRecusarEnderecoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, null, "Sabor", START, END));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa nome em branco")
    void deveRecusarNomeEmBranco(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS_ID, name, START, END));
    }

    @Test
    @DisplayName("Recusa horário de abertura nulo")
    void deveRecusarHorarioAberturaNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS_ID, "Sabor", null, END));
    }

    @Test
    @DisplayName("Recusa horário de fechamento nulo")
    void deveRecusarHorarioFechamentoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS_ID, "Sabor", START, null));
    }

    @Test
    @DisplayName("Recusa horários iguais")
    void deveRecusarHorariosIguais() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS_ID, "Sabor", START, START));
    }

    @Test
    @DisplayName("Revalida nos setters")
    void deveRevalidarNosSetters() {
        Restaurant restaurant = Restaurant.create(USER_ID, ADDRESS_ID, "Sabor", START, END);
        UUID novoUser = UUID.randomUUID();
        UUID novoAddr = UUID.randomUUID();

        restaurant.setUserId(novoUser);
        restaurant.setAddressId(novoAddr);
        restaurant.setName("Novo Nome");
        restaurant.setOfficeHours(LocalTime.of(9, 0), LocalTime.of(23, 0));

        assertEquals(novoUser, restaurant.getUserId());
        assertEquals(novoAddr, restaurant.getAddressId());
        assertEquals("Novo Nome", restaurant.getName());
        assertEquals(LocalTime.of(9, 0), restaurant.getOfficeHourStart());
        assertEquals(LocalTime.of(23, 0), restaurant.getOfficeHourEnd());
    }
}
