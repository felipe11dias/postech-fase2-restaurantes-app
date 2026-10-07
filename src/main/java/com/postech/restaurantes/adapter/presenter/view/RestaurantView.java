package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Visão externa do restaurante produzida pelo presenter. */
public record RestaurantView(
        UUID id,
        UUID userId,
        AddressView address,
        String name,
        List<OfficeHourView> officeHours,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}
