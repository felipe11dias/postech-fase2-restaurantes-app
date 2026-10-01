package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;

/** Prepara a saída do restaurante para views. */
public final class RestaurantPresenter {

    private RestaurantPresenter() {
    }

    public static RestaurantView toView(Restaurant restaurant) {
        Guard.requireNonNull(restaurant, "Restaurante inválido");
        return new RestaurantView(
                restaurant.getId(),
                restaurant.getUserId(),
                restaurant.getAddressId(),
                restaurant.getName(),
                restaurant.getOfficeHourStart(),
                restaurant.getOfficeHourEnd(),
                restaurant.getCreatedAt(),
                restaurant.getLastUpdatedAt()
        );
    }

    public static PageResult<RestaurantView> toView(PageResult<Restaurant> page) {
        Guard.requireNonNull(page, "Página inválida");
        return page.map(RestaurantPresenter::toView);
    }
}
