package com.postech.restaurantes.application.dto.restaurant;

import java.time.LocalTime;
import java.util.UUID;

/** Dados de entrada para o caso de uso de criação de restaurante. */
public record CreateRestaurantDTO(
        UUID userId,
        UUID addressId,
        String name,
        LocalTime officeHourStart,
        LocalTime officeHourEnd
) {
}
