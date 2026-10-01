package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/** Restaurante no corpo da resposta HTTP. */
public record RestaurantResponse(
        UUID id,
        UUID userId,
        UUID addressId,
        String name,
        LocalTime officeHourStart,
        LocalTime officeHourEnd,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {

    public static RestaurantResponse from(RestaurantView view) {
        return new RestaurantResponse(
                view.id(),
                view.userId(),
                view.addressId(),
                view.name(),
                view.officeHourStart(),
                view.officeHourEnd(),
                view.createdAt(),
                view.lastUpdatedAt()
        );
    }
}
