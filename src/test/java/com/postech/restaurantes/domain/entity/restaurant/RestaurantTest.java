package com.postech.restaurantes.domain.entity.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.entity.address.Address;
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
    private static final Address ADDRESS =
            Address.create("Rua das Flores", "100", null, "Centro", "São Paulo", "SP", "01001000");
    private static final LocalTime START = LocalTime.of(8, 0);
    private static final LocalTime END = LocalTime.of(22, 0);

    @Test
    @DisplayName("Cria restaurante válido sem id nem auditoria")
    void deveCriarQuandoValido() {
        Restaurant restaurant = Restaurant.create(USER_ID, ADDRESS, "Sabor & Arte", START, END);

        assertNull(restaurant.getId());
        assertNull(restaurant.getCreatedAt());
        assertNull(restaurant.getLastUpdatedAt());
        assertEquals(USER_ID, restaurant.getUserId());
        assertSame(ADDRESS, restaurant.getAddress());
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

        Restaurant restaurant = Restaurant.restore(id, USER_ID, ADDRESS, "Sabor & Arte", START, END, criado, alterado);

        assertEquals(id, restaurant.getId());
        assertEquals(criado, restaurant.getCreatedAt());
        assertEquals(alterado, restaurant.getLastUpdatedAt());
    }

    @Test
    @DisplayName("Recusa restauração com id nulo")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.restore(null, USER_ID, ADDRESS, "Sabor", START, END, null, null));
    }

    @Test
    @DisplayName("Recusa dono nulo")
    void deveRecusarDonoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(null, ADDRESS, "Sabor", START, END));
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
                () -> Restaurant.create(USER_ID, ADDRESS, name, START, END));
    }

    @Test
    @DisplayName("Recusa horário de abertura nulo")
    void deveRecusarHorarioAberturaNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", null, END));
    }

    @Test
    @DisplayName("Recusa horário de fechamento nulo")
    void deveRecusarHorarioFechamentoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", START, null));
    }

    @Test
    @DisplayName("Recusa horários iguais")
    void deveRecusarHorariosIguais() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", START, START));
    }

    @Test
    @DisplayName("Revalida nos setters")
    void deveRevalidarNosSetters() {
        Restaurant restaurant = Restaurant.create(USER_ID, ADDRESS, "Sabor", START, END);
        UUID novoUser = UUID.randomUUID();
        Address novoEndereco = Address.create("Av. B", null, null, null, "Rio", "RJ", "20000000");

        restaurant.setUserId(novoUser);
        restaurant.setAddress(novoEndereco);
        restaurant.setName("Novo Nome");
        restaurant.setOfficeHours(LocalTime.of(9, 0), LocalTime.of(23, 0));

        assertEquals(novoUser, restaurant.getUserId());
        assertSame(novoEndereco, restaurant.getAddress());
        assertEquals("Novo Nome", restaurant.getName());
        assertEquals(LocalTime.of(9, 0), restaurant.getOfficeHourStart());
        assertEquals(LocalTime.of(23, 0), restaurant.getOfficeHourEnd());
    }
}
