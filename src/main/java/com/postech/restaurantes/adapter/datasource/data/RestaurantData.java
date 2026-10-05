package com.postech.restaurantes.adapter.datasource.data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Restaurante como a origem de dados o conhece. Em inserções, id e auditoria vêm nulos e a
 * origem de dados devolve o registro preenchido.
 */
public record RestaurantData(
        UUID id,
        UUID userId,
        AddressData address,
        String name,
        LocalTime officeHourStart,
        LocalTime officeHourEnd,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}
