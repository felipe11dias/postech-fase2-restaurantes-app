package com.postech.restaurantes.adapter.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantPresenterTest {

    @Test
    @DisplayName("Converte entidade para view")
    void deveConverterParaView() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        Restaurant restaurant = Restaurant.restore(id, userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0),
                LocalDateTime.now(), LocalDateTime.now());

        RestaurantView view = RestaurantPresenter.toView(restaurant);

        assertEquals(id, view.id());
        assertEquals("Sabor", view.name());
    }

    @Test
    @DisplayName("Converte pagina de entidades para pagina de views")
    void deveConverterPagina() {
        UUID id = UUID.randomUUID();
        Restaurant restaurant = Restaurant.restore(id, UUID.randomUUID(), UUID.randomUUID(), "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0),
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
