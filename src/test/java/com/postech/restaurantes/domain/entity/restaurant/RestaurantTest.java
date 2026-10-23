package com.postech.restaurantes.domain.entity.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.entity.address.Address;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final OfficeHour SEGUNDA = new OfficeHour(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(22, 0));
    private static final List<OfficeHour> HORARIOS = List.of(SEGUNDA);

    @Test
    @DisplayName("Cria restaurante válido sem id nem auditoria")
    void deveCriarQuandoValido() {
        Restaurant restaurant = Restaurant.create(USER_ID, ADDRESS, "Sabor & Arte", HORARIOS);

        assertNull(restaurant.getId());
        assertNull(restaurant.getCreatedAt());
        assertNull(restaurant.getLastUpdatedAt());
        assertEquals(USER_ID, restaurant.getUserId());
        assertSame(ADDRESS, restaurant.getAddress());
        assertEquals("Sabor & Arte", restaurant.getName());
        assertEquals(HORARIOS, restaurant.getOfficeHours());
    }

    @Test
    @DisplayName("Restaura restaurante com id e auditoria conhecidos")
    void deveRestaurarComIdEAuditoria() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime alterado = LocalDateTime.of(2026, 1, 2, 10, 0);

        Restaurant restaurant = Restaurant.restore(id, USER_ID, ADDRESS, "Sabor & Arte", HORARIOS, criado, alterado);

        assertEquals(id, restaurant.getId());
        assertEquals(criado, restaurant.getCreatedAt());
        assertEquals(alterado, restaurant.getLastUpdatedAt());
    }

    @Test
    @DisplayName("Recusa restauração com id nulo")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.restore(null, USER_ID, ADDRESS, "Sabor", HORARIOS, null, null));
    }

    @Test
    @DisplayName("Recusa dono nulo")
    void deveRecusarDonoNulo() {
        assertThrows(IllegalArgumentException.class, () -> Restaurant.create(null, ADDRESS, "Sabor", HORARIOS));
    }

    @Test
    @DisplayName("Recusa endereço nulo")
    void deveRecusarEnderecoNulo() {
        assertThrows(IllegalArgumentException.class, () -> Restaurant.create(USER_ID, null, "Sabor", HORARIOS));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa nome em branco")
    void deveRecusarNomeEmBranco(String name) {
        assertThrows(IllegalArgumentException.class, () -> Restaurant.create(USER_ID, ADDRESS, name, HORARIOS));
    }

    @Test
    @DisplayName("Recusa lista de horários nula, vazia ou com horário nulo")
    void deveRecusarHorariosAusentes() {
        List<OfficeHour> comNulo = new ArrayList<>(Arrays.asList(SEGUNDA, null));

        assertThrows(IllegalArgumentException.class, () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", null));
        IllegalArgumentException vazia =
                assertThrows(IllegalArgumentException.class, () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", comNulo));
        assertEquals("Restaurante deve ter ao menos um horário de funcionamento", vazia.getMessage());
    }

    @Test
    @DisplayName("Recusa horários sobrepostos, inclusive o que vira a meia-noite e invade o dia seguinte")
    void deveRecusarHorariosSobrepostos() {
        OfficeHour segundaAteMadrugada = new OfficeHour(DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(2, 0));
        OfficeHour tercaCedo = new OfficeHour(DayOfWeek.TUESDAY, LocalTime.of(1, 0), LocalTime.of(10, 0));
        OfficeHour segundaTarde = new OfficeHour(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(23, 0));

        IllegalArgumentException madrugada = assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", List.of(segundaAteMadrugada, tercaCedo)));
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", List.of(SEGUNDA, segundaTarde)));
        assertThrows(IllegalArgumentException.class,
                () -> Restaurant.create(USER_ID, ADDRESS, "Sabor", List.of(SEGUNDA, SEGUNDA)));
        assertEquals("Os horários de funcionamento não podem se sobrepor", madrugada.getMessage());
    }

    @Test
    @DisplayName("Aceita vários intervalos no mesmo dia e os guarda na ordem da semana")
    void deveOrdenarOsHorarios() {
        OfficeHour domingo = new OfficeHour(DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(15, 0));
        OfficeHour segundaJantar = new OfficeHour(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(23, 0));
        OfficeHour segundaAlmoco = new OfficeHour(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(15, 0));

        Restaurant restaurant = Restaurant.create(USER_ID, ADDRESS, "Sabor", List.of(domingo, segundaJantar, segundaAlmoco));

        assertEquals(List.of(segundaAlmoco, segundaJantar, domingo), restaurant.getOfficeHours());
        assertThrows(UnsupportedOperationException.class, () -> restaurant.getOfficeHours().clear());
    }

    @Test
    @DisplayName("Revalida nos setters e na troca dos horários")
    void deveRevalidarNosSetters() {
        Restaurant restaurant = Restaurant.create(USER_ID, ADDRESS, "Sabor", HORARIOS);
        UUID novoUser = UUID.randomUUID();
        Address novoEndereco = Address.create("Av. B", null, null, null, "Rio", "RJ", "20000000");
        List<OfficeHour> novos = List.of(new OfficeHour(DayOfWeek.FRIDAY, LocalTime.of(18, 0), LocalTime.of(2, 0)));

        restaurant.setUserId(novoUser);
        restaurant.setAddress(novoEndereco);
        restaurant.setName("Novo Nome");
        restaurant.replaceOfficeHours(novos);

        assertEquals(novoUser, restaurant.getUserId());
        assertSame(novoEndereco, restaurant.getAddress());
        assertEquals("Novo Nome", restaurant.getName());
        assertEquals(novos, restaurant.getOfficeHours());
        assertThrows(IllegalArgumentException.class, () -> restaurant.replaceOfficeHours(List.of()));
        assertEquals(novos, restaurant.getOfficeHours(), "troca recusada não muda os horários");
    }
}
