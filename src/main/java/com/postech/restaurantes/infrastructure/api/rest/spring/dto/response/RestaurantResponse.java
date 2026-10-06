package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Restaurante no corpo da resposta HTTP. */
public record RestaurantResponse(
        UUID id,
        UUID userId,
        AddressResponse address,
        String name,
        List<OfficeHourResponse> officeHours,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {

    public static RestaurantResponse from(RestaurantView view) {
        return new RestaurantResponse(
                view.id(),
                view.userId(),
                AddressResponse.from(view.address()),
                view.name(),
                view.officeHours().stream().map(OfficeHourResponse::from).toList(),
                view.createdAt(),
                view.lastUpdatedAt()
        );
    }
}
