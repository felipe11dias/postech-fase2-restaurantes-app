package com.postech.restaurantes.adapter.presenter;

import static com.postech.restaurantes.adapter.AdapterFixtures.OFFICE_HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantPresenterTest {

    private static final Address ENDERECO = Address.restore(UUID.randomUUID(), "Rua A", "10", null, "Bairro", "Cidade",
            "SP", "01000000");

    @Test
    @DisplayName("Converte entidade para view")
    void deveConverterParaView() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Restaurant restaurant = Restaurant.restore(id, userId, ENDERECO, "Sabor", OFFICE_HOURS,
                LocalDateTime.now(), LocalDateTime.now());

        RestaurantView view = RestaurantPresenter.toView(restaurant);

        assertEquals(id, view.id());
        assertEquals("Sabor", view.name());
        assertEquals(ENDERECO.getId(), view.address().id());
        assertEquals("01000000", view.address().zipCode());
    }

    @Test
    @DisplayName("Converte pagina de entidades para pagina de views")
    void deveConverterPagina() {
        UUID id = UUID.randomUUID();
        Restaurant restaurant = Restaurant.restore(id, UUID.randomUUID(), ENDERECO, "Sabor", OFFICE_HOURS,
                null, null);
        PageResult<Restaurant> page = new PageResult<>(List.of(restaurant), 0, 10, 1);

        PageResult<RestaurantView> viewPage = RestaurantPresenter.toView(page);

        assertEquals(1, viewPage.totalElements());
    }

    @Test
    @DisplayName("Recusa parametros nulos")
    void deveRecusarParametrosNulos() {
        assertThrows(IllegalArgumentException.class, () -> RestaurantPresenter.toView((Restaurant) null));
        assertThrows(IllegalArgumentException.class, () -> RestaurantPresenter.toView((PageResult<Restaurant>) null));
    }
}
