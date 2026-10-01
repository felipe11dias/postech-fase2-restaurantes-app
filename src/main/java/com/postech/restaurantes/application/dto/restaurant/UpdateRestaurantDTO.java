package com.postech.restaurantes.application.dto.restaurant;

import java.time.LocalTime;
import java.util.UUID;

/** Dados de entrada para o caso de uso de atualização de restaurante. */
public record UpdateRestaurantDTO(
        UUID id,
        UUID userId,
        UUID addressId,
        String name,
        LocalTime officeHourStart,
        LocalTime officeHourEnd
) {
}
