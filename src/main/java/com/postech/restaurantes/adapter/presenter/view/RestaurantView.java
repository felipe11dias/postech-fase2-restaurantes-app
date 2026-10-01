package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/** Visão externa do restaurante produzida pelo presenter. */
public record RestaurantView(
        UUID id,
        UUID userId,
        UUID addressId,
        String name,
        LocalTime officeHourStart,
        LocalTime officeHourEnd,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}
